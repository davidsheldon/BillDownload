package com.bitclean.billscrape.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;

/**
 */
public class IOUtils {
  public static void saveUrl(final File savefile, final String pdfUrl, final MyHtmlUnitDriver driver) throws IOException {
    savefile.getParentFile().mkdirs();
    
    final InputStream is = driver.client().getPage(pdfUrl).getWebResponse().getContentAsStream();


    OutputStream os = new FileOutputStream(savefile);
    int i;
    while ((i = is.read()) != -1) {
      os.write(i);
    }
    os.close();
    is.close();
  }
}
