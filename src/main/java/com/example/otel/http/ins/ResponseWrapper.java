package com.example.otel.http.ins;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;

/**
 * 响应包装器，用于捕获响应体内容
 */
public class ResponseWrapper extends HttpServletResponseWrapper {

    private final ByteArrayOutputStream capture;
    private ServletOutputStream output;
    private PrintWriter writer;

    public ResponseWrapper(HttpServletResponse response) {
        super(response);
        capture = new ByteArrayOutputStream();
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        if (writer != null) {
            throw new IllegalStateException("getWriter() has already been called on this response.");
        }

        if (output == null) {
            output = new ServletOutputStream() {
                @Override
                public boolean isReady() {
                    return false;
                }

                @Override
                public void setWriteListener(WriteListener writeListener) {

                }

                @Override
                public void write(int b) throws IOException {
                    capture.write(b);
                    getResponse().getOutputStream().write(b);
                }

                @Override
                public void write(byte[] b) throws IOException {
                    capture.write(b);
                    getResponse().getOutputStream().write(b);
                }

                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    capture.write(b, off, len);
                    getResponse().getOutputStream().write(b, off, len);
                }
            };
        }
        return output;
    }


    @Override
    public PrintWriter getWriter() throws IOException {
        if (output != null) {
            throw new IllegalStateException("getOutputStream() has already been called on this response.");
        }

        if (writer == null) {
            String characterEncoding = getCharacterEncoding();
            if (characterEncoding == null) {
                characterEncoding = "UTF-8";
            }

            writer = new PrintWriter(new OutputStreamWriter(capture, characterEncoding)) {
                @Override
                public void write(char[] buf, int off, int len) {
                    super.write(buf, off, len);
                    super.flush();
                }

                @Override
                public void write(String s, int off, int len) {
                    super.write(s, off, len);
                    super.flush();
                }

                @Override
                public void write(int c) {
                    super.write(c);
                    super.flush();
                }
            };
        }
        return writer;
    }

    @Override
    public void flushBuffer() throws IOException {
        if (writer != null) {
            writer.flush();
        } else if (output != null) {
            output.flush();
        }
        super.flushBuffer();
    }

    /**
     * 获取捕获的响应体内容
     */
    public String getCapturedContent() {
        try {
            if (writer != null) {
                writer.close();
            } else if (output != null) {
                output.close();
            }

            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = "UTF-8";
            }

            return capture.toString(encoding);
        } catch (Exception e) {
            // 如果出现任何异常，返回默认编码的字符串
            return capture.toString();
        }
    }

    /**
     * 获取响应体内容的字节数组
     */
    public byte[] getCapturedContentAsBytes() {
        return capture.toByteArray();
    }

    /**
     * 检查响应体是否为空
     */
    public boolean hasContent() {
        return capture.size() > 0;
    }

    /**
     * 获取响应体大小
     */
    public int getContentSize() {
        return capture.size();
    }
}
