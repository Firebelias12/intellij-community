/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author tav
 */
public class SVGLoader {
  private TranscoderInput input;
  private BufferedImage img;
  private float width;
  private float height;

  private enum SizeAttr {
    width,
    height;

    static final int FALLBACK_VALUE = 16;

    public int value(@NotNull Document document) {
      String value = document.getDocumentElement().getAttribute(name());
      assert value.endsWith("px") : "unexpected '" + name() + "' format in " + document.getBaseURI();
      try {
        return Integer.parseInt(value.substring(0, value.length() - 2));
      }
      catch (NumberFormatException ex) {
        ex.printStackTrace();
        return FALLBACK_VALUE;
      }
    }
  }

  private class MyTranscoder extends ImageTranscoder {
    @Override
    public BufferedImage createImage(int w, int h) {
      return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    }

    @Override
    public void writeImage(BufferedImage img, TranscoderOutput output) throws TranscoderException {
      SVGLoader.this.img = img;
    }
  }

  public static Image load(@NotNull URL url, float scale) throws IOException {
    return load(url, url.openStream(), scale);
  }

  public static Image load(@NotNull InputStream stream , float scale) throws IOException {
    return load(null, stream, scale);
  }

  public static Image load(@Nullable URL url, @NotNull InputStream stream , float scale) throws IOException {
    try {
      return new SVGLoader(url, stream, scale).createImage();
    }
    catch (TranscoderException ex) {
      throw new IOException(ex);
    }
  }

  private SVGLoader(@Nullable URL url, InputStream stream, float scale) throws IOException {
    Document document = null;
    String uri = null;
    try {
      uri = url != null ? url.toURI().toString() : null;
    }
    catch (URISyntaxException ignore) {
    }
    document = new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName()).
      createDocument(uri, stream);
    if (document == null) {
      throw new IOException("document not created");
    }
    input = new TranscoderInput(document);

    width = SizeAttr.width.value(document) * scale;
    height = SizeAttr.height.value(document) * scale;
  }

  private BufferedImage createImage() throws TranscoderException {
    MyTranscoder r = new MyTranscoder();
    r.addTranscodingHint(ImageTranscoder.KEY_WIDTH, new Float(width));
    r.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, new Float(height));
    r.transcode(input, null);
    return img;
  }
}
