/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.internal.RouteAnalyzer;
import io.jooby.internal.RouterImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class App implements Router {

  private final RouterImpl router;

  private Path tmpdir = Paths.get(System.getProperty("java.io.tmpdir"), appname(getClass()))
      .toAbsolutePath();

  private ExecutionMode mode = ExecutionMode.DEFAULT;

  public App() {
    router = new RouterImpl(new RouteAnalyzer(getClass().getClassLoader(), false));
  }

  @Nonnull @Override public App basePath(String basePath) {
    router.basePath(basePath);
    return this;
  }

  @Nonnull @Override public String basePath() {
    return router.basePath();
  }

  @Nonnull @Override
  public App use(@Nonnull Router router) {
    this.router.use(router);
    return this;
  }

  @Nonnull @Override
  public App use(@Nonnull Predicate<Context> predicate, @Nonnull Router router) {
    this.router.use(predicate, router);
    return this;
  }

  @Nonnull @Override public App use(@Nonnull String path, @Nonnull Router router) {
    this.router.use(path, router);
    return this;
  }

  @Nonnull @Override public List<Route> routes() {
    return router.routes();
  }

  @Nonnull @Override public App gzip(@Nonnull Runnable action) {
    router.gzip(action);
    return this;
  }

  @Nonnull @Override public App error(@Nonnull Route.ErrorHandler handler) {
    router.error(handler);
    return this;
  }

  @Nonnull @Override public App filter(@Nonnull Route.Filter filter) {
    router.filter(filter);
    return this;
  }

  @Nonnull @Override public App before(@Nonnull Route.Before before) {
    router.before(before);
    return this;
  }

  @Nonnull @Override public App after(@Nonnull Route.After after) {
    router.after(after);
    return this;
  }

  @Nonnull @Override public App renderer(@Nonnull Renderer renderer) {
    router.renderer(renderer);
    return this;
  }

  @Nonnull @Override public App stack(@Nonnull Runnable action) {
    router.stack(action);
    return this;
  }

  @Nonnull @Override public Router stack(@Nonnull Executor executor, @Nonnull Runnable action) {
    router.stack(executor, action);
    return this;
  }

  @Nonnull @Override public App path(@Nonnull String pattern, @Nonnull Runnable action) {
    router.path(pattern, action);
    return this;
  }

  @Nonnull @Override
  public Route route(@Nonnull String method, @Nonnull String pattern,
      @Nonnull Route.Handler handler) {
    return router.route(method, pattern, handler);
  }

  @Nonnull @Override public Match match(@Nonnull Context ctx) {
    return router.match(ctx);
  }

  /** Error handler: */
  @Nonnull @Override
  public App errorCode(@Nonnull Class<? extends Throwable> type,
      @Nonnull StatusCode statusCode) {
    router.errorCode(type, statusCode);
    return this;
  }

  @Nonnull @Override public Executor worker() {
    return router.worker();
  }

  @Nonnull @Override public App worker(Executor worker) {
    this.router.worker(worker);
    return this;
  }

  /** Log: */
  public Logger log() {
    return LoggerFactory.getLogger(getClass());
  }

  @Nonnull @Override public Route.RootErrorHandler errorHandler() {
    return router.errorHandler();
  }

  public Path tmpdir() {
    return tmpdir;
  }

  public App tmpdir(@Nonnull Path tmpdir) {
    this.tmpdir = tmpdir;
    return this;
  }

  public ExecutionMode mode() {
    return mode;
  }

  public App mode(ExecutionMode mode) {
    this.mode = mode;
    return this;
  }

  /** Boot: */
  public Server start() {
    List<Server> servers = ServiceLoader.load(Server.class).stream()
        .map(ServiceLoader.Provider::get)
        .collect(Collectors.toList());
    if (servers.size() == 0) {
      throw new IllegalStateException("Server not found.");
    }
    if (servers.size() > 1) {
      List<String> names = servers.stream()
          .map(it -> it.getClass().getSimpleName().toLowerCase())
          .collect(Collectors.toList());
      log().warn("Multiple servers found {}. Using: {}", names, names.get(0));
    }
    return servers.get(0)
        .deploy(this)
        .start();
  }

  public App start(Server server) {
    /** Start router: */
    ensureTmpdir(tmpdir);
    Logger log = log();
    router.start(this);
    log.info("{} [{}@{}]\n\n{}\n\nlistening on:\n  http://localhost:{}{}\n",
        getClass().getSimpleName(),
        server.getClass().getSimpleName().toLowerCase(), mode.name().toLowerCase(), router,
        server.port(), router.basePath());
    return this;
  }

  public App stop() {
    router.destroy();
    return this;
  }

  @Override public String toString() {
    return router.toString();
  }

  private static void ensureTmpdir(Path tmpdir) {
    try {
      if (!Files.exists(tmpdir)) {
        Files.createDirectories(tmpdir);
      }
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }

  private static String appname(Class<?> clazz) {
    String[] segments = clazz.getName().split("\\.");
    return segments.length == 1 ? segments[0] : segments[segments.length - 2];
  }
}
