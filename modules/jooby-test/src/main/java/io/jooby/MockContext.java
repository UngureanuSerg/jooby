/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * Unit test friendly context implementation. Allows to set context properties.
 */
public class MockContext implements DefaultContext {

  private String method = Router.GET;

  private Route route;

  private String pathString = "/";

  private Map<String, String> pathMap = new HashMap<>();

  private String queryString;

  private Map<String, Collection<String>> headers = new HashMap<>();

  private Formdata formdata = Formdata.create();

  private Multipart multipart = Multipart.create();

  private Body body;

  private Object bodyObject;

  private Map<String, MessageDecoder> decoders = new HashMap<>();

  private Map<String, Object> responseHeaders = new HashMap<>();

  private Map<String, Object> attributes = new HashMap<>();

  private MockResponse response = new MockResponse();

  private Map<String, String> cookies = new LinkedHashMap<>();

  private FlashMap flashMap = FlashMap.create(this, new Cookie("jooby.sid").setHttpOnly(true));

  private Session session;

  private Router router;

  private List<FileUpload> files = new ArrayList<>();

  private boolean responseStarted;

  private boolean resetHeadersOnError = true;

  @Nonnull @Override public String getMethod() {
    return method;
  }

  MockContext setMethod(@Nonnull String method) {
    this.method = method;
    return this;
  }

  @Nonnull @Override public Session session() {
    if (session == null) {
      session = new MockSession();
    }
    return session;
  }

  @Nullable @Override public Session sessionOrNull() {
    return session;
  }

  @Nonnull @Override public Map<String, String> cookieMap() {
    return cookies;
  }

  /**
   * Set cookie map.
   *
   * @param cookies Cookie map.
   * @return This context.
   */
  @Nonnull public MockContext setCookieMap(@Nonnull Map<String, String> cookies) {
    this.cookies = cookies;
    return this;
  }

  @Nonnull @Override public FlashMap flash() {
    return flashMap;
  }

  /**
   * Set flash map.
   *
   * @param flashMap Flash map.
   * @return This context.
   */
  public MockContext setFlashMap(@Nonnull FlashMap flashMap) {
    this.flashMap = flashMap;
    return this;
  }

  /**
   * Set request flash attribute.
   *
   * @param name Flash name.
   * @param value Flash value.
   * @return This context.
   */
  @Nonnull public MockContext setFlashAttribute(@Nonnull String name, @Nonnull String value) {
    flashMap.put(name, value);
    return this;
  }

  @Nonnull @Override public Route getRoute() {
    return route;
  }

  @Nonnull @Override public MockContext setRoute(@Nonnull Route route) {
    this.route = route;
    return this;
  }

  @Nonnull @Override public String pathString() {
    return pathString;
  }

  /**
   * Set pathString and queryString (if any).
   *
   * @param pathString Path string.
   * @return This context.
   */
  public MockContext setPathString(@Nonnull String pathString) {
    int q = pathString.indexOf("?");
    if (q > 0) {
      this.pathString = pathString.substring(0, q);
      this.queryString = pathString.substring(q + 1);
    } else {
      this.pathString = pathString;
      this.queryString = null;
    }
    return this;
  }

  @Nonnull @Override public Map<String, String> pathMap() {
    return pathMap;
  }

  @Nonnull @Override public MockContext setPathMap(@Nonnull Map<String, String> pathMap) {
    this.pathMap = pathMap;
    return this;
  }

  @Nonnull @Override public QueryString query() {
    return QueryString.create(queryString);
  }

  @Nonnull @Override public String queryString() {
    return queryString;
  }

  @Nonnull @Override public Value header() {
    return Value.hash(headers);
  }

  /**
   * Set request headers.
   *
   * @param headers Request headers.
   * @return This context.
   */
  @Nonnull public MockContext setHeaders(@Nonnull Map<String, Collection<String>> headers) {
    this.headers = headers;
    return this;
  }

  /**
   * Set request headers.
   *
   * @param name Request header.
   * @param value Request value.
   * @return This context.
   */
  @Nonnull public MockContext setRequestHeader(@Nonnull String name, @Nonnull String value) {
    Collection<String> values = this.headers.computeIfAbsent(name, k -> new ArrayList<>());
    values.add(value);
    return this;
  }

  @Nonnull @Override public Formdata form() {
    return formdata;
  }

  /**
   * Set formdata.
   *
   * @param formdata Formdata.
   * @return This context.
   */
  @Nonnull public MockContext setForm(@Nonnull Formdata formdata) {
    this.formdata = formdata;
    return this;
  }

  @Nonnull @Override public Multipart multipart() {
    return multipart;
  }

  @Nonnull @Override public List<FileUpload> files() {
    return files;
  }

  /**
   * Set mock files.
   *
   * @param files Mock files.
   * @return This context.
   */
  public MockContext setFiles(@Nonnull List<FileUpload> files) {
    this.files = files;
    return this;
  }

  @Nonnull @Override public List<FileUpload> files(@Nonnull String name) {
    return files.stream().filter(it -> it.name().equals(name)).collect(Collectors.toList());
  }

  @Nonnull @Override public FileUpload file(@Nonnull String name) {
    return files.stream().filter(it -> it.name().equals(name)).findFirst()
        .orElseThrow(() -> new TypeMismatchException(name, FileUpload.class));
  }

  /**
   * Set multipart.
   *
   * @param multipart Multipart.
   * @return This context.
   */
  @Nonnull public MockContext setMultipart(@Nonnull Multipart multipart) {
    this.multipart = multipart;
    return this;
  }

  @Nonnull @Override public Body body() {
    if (body == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    return body;
  }

  @Nonnull @Override public <T> T body(@Nonnull Reified<T> type) {
    if (bodyObject == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    if (!type.getRawType().isInstance(bodyObject)) {
      throw new TypeMismatchException("body", FileUpload.class);
    }
    return body(type, MediaType.text);
  }

  @Nonnull @Override public <T> T body(@Nonnull Reified<T> type, @Nonnull MediaType contentType) {
    if (bodyObject == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    if (!type.getRawType().isInstance(bodyObject)) {
      throw new TypeMismatchException("body", type.getType());
    }
    return (T) type.getRawType().cast(bodyObject);
  }

  @Nonnull @Override public <T> T body(@Nonnull Class<T> type) {
    return body(type, MediaType.text);
  }

  @Nonnull @Override public <T> T body(@Nonnull Class<T> type, @Nonnull MediaType contentType) {
    if (bodyObject == null) {
      throw new IllegalStateException("No body was set, use setBody() to set one.");
    }
    if (!type.isInstance(bodyObject)) {
      throw new TypeMismatchException("body", type);
    }
    return type.cast(bodyObject);
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @Nonnull public MockContext setBody(@Nonnull Object body) {
    if (body instanceof Body) {
      this.body = (Body) body;
    } else {
      this.bodyObject = body;
    }
    return this;
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @Nonnull public MockContext setBody(@Nonnull String body) {
    byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
    return setBody(bytes);
  }

  /**
   * Set request body.
   *
   * @param body Request body.
   * @return This context.
   */
  @Nonnull public MockContext setBody(@Nonnull byte[] body) {
    this.body = Body.of(new ByteArrayInputStream(body), body.length);
    return this;
  }

  @Nonnull @Override public MessageDecoder decoder(@Nonnull MediaType contentType) {
    return decoders.getOrDefault(contentType, MessageDecoder.UNSUPPORTED_MEDIA_TYPE);
  }

  @Override public boolean isInIoThread() {
    return false;
  }

  @Nonnull @Override public MockContext dispatch(@Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override
  public MockContext dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
    action.run();
    return this;
  }

  @Nonnull @Override public MockContext detach(@Nonnull Route.Handler next) throws Exception {
    next.apply(this);
    return this;
  }

  @Nonnull @Override public Map<String, Object> getAttributes() {
    return attributes;
  }

  @Nonnull @Override public MockContext removeResponseHeader(@Nonnull String name) {
    responseHeaders.remove(name);
    return this;
  }

  @Nonnull @Override
  public MockContext setResponseHeader(@Nonnull String name, @Nonnull String value) {
    responseHeaders.put(name, value);
    return this;
  }

  @Nonnull @Override public MockContext setResponseLength(long length) {
    response.setContentLength(length);
    return this;
  }

  @Override public long getResponseLength() {
    return response.getContentLength();
  }

  @Nonnull @Override public MockContext setResponseType(@Nonnull String contentType) {
    response.setContentType(MediaType.valueOf(contentType));
    return this;
  }

  @Nonnull @Override
  public MockContext setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset) {
    response.setContentType(contentType);
    return this;
  }

  @Nonnull @Override public MockContext setResponseCode(int statusCode) {
    response.setStatusCode(StatusCode.valueOf(statusCode));
    return this;
  }

  @Nonnull @Override public StatusCode getResponseCode() {
    return response.getStatusCode();
  }

  @Nonnull @Override public MockContext render(@Nonnull Object result) {
    responseStarted = true;
    this.response.setResult(result);
    return this;
  }

  /**
   * Mock response generated from route execution.
   *
   * @return Mock response.
   */
  @Nonnull public MockResponse getResponse() {
    responseStarted = true;
    response.setHeaders(responseHeaders);
    return response;
  }

  @Nonnull @Override public OutputStream responseStream() {
    responseStarted = true;
    ByteArrayOutputStream out = new ByteArrayOutputStream(ServerOptions._16KB);
    this.response.setResult(out);
    return out;
  }

  @Nonnull @Override public Sender responseSender() {
    responseStarted = true;
    return new Sender() {
      @Override public Sender write(@Nonnull byte[] data, @Nonnull Callback callback) {
        response.setResult(data);
        callback.onComplete(MockContext.this, null);
        return this;
      }

      @Override public void close() {
      }
    };
  }

  @Nonnull @Override public String getRemoteAddress() {
    return "0.0.0.0";
  }

  @Nonnull @Override public String getProtocol() {
    return "HTTP/1.1";
  }

  @Nonnull @Override public String getScheme() {
    return "http";
  }

  @Nonnull @Override public PrintWriter responseWriter(MediaType type, Charset charset) {
    responseStarted = true;
    PrintWriter writer = new PrintWriter(new StringWriter());
    this.response.setResult(writer)
        .setContentType(type);
    return writer;
  }

  @Nonnull @Override public MockContext send(@Nonnull String data, @Nonnull Charset charset) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.length());
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull byte[] data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.length);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull ByteBuffer data) {
    responseStarted = true;
    this.response.setResult(data)
        .setContentLength(data.remaining());
    return this;
  }

  @Nonnull @Override public MockContext send(InputStream input) {
    responseStarted = true;
    this.response.setResult(input);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull AttachedFile file) {
    responseStarted = true;
    this.response.setResult(file);
    return this;
  }

  @Nonnull @Override public Context send(@Nonnull Path file) {
    responseStarted = true;
    this.response.setResult(file);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull ReadableByteChannel channel) {
    responseStarted = true;
    this.response.setResult(channel);
    return this;
  }

  @Nonnull @Override public MockContext send(@Nonnull FileChannel file) {
    responseStarted = true;
    this.response.setResult(file);
    return this;
  }

  @Nonnull @Override public MockContext send(StatusCode statusCode) {
    responseStarted = true;
    this.response
        .setContentLength(0)
        .setStatusCode(statusCode);
    return this;
  }

  @Nonnull @Override public MockContext sendError(@Nonnull Throwable cause) {
    return sendError(cause, router.errorCode(cause));
  }

  @Nonnull @Override
  public MockContext sendError(@Nonnull Throwable cause, @Nonnull StatusCode statusCode) {
    responseStarted = true;
    this.response.setResult(cause)
        .setStatusCode(router.errorCode(cause));
    return this;
  }

  @Nonnull @Override public MockContext setDefaultResponseType(@Nonnull MediaType contentType) {
    response.setContentType(contentType);
    return this;
  }

  @Nonnull @Override public MockContext setResponseCookie(@Nonnull Cookie cookie) {
    String setCookie = (String) response.getHeaders().get("Set-Cookie");
    if (setCookie == null) {
      setCookie = cookie.toCookieString();
    } else {
      setCookie += ";" + cookie.toCookieString();
    }
    response.setHeader("Set-Cookie", setCookie);
    return this;
  }

  @Nonnull @Override public MediaType getResponseType() {
    return response.getContentType();
  }

  @Nonnull @Override public MockContext setResponseCode(@Nonnull StatusCode statusCode) {
    response.setStatusCode(statusCode);
    return this;
  }

  @Override public boolean isResponseStarted() {
    return responseStarted;
  }

  @Override public boolean getResetHeadersOnError() {
    return resetHeadersOnError;
  }

  @Override public MockContext setResetHeadersOnError(boolean resetHeadersOnError) {
    this.resetHeadersOnError = resetHeadersOnError;
    return this;
  }

  @Nonnull @Override public Context removeResponseHeaders() {
    responseHeaders.clear();
    return this;
  }

  @Nonnull @Override public Router getRouter() {
    return router;
  }

  @Nonnull MockContext setRouter(@Nonnull Router router) {
    this.router = router;
    return this;
  }

  @Override public String toString() {
    return method + " " + pathString;
  }
}
