package com.bitclean.billscrape;

import java.io.File;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.text.MessageFormat;

/**
 */
public abstract class DefaultOptions implements ScraperDefinition {
  boolean overwrite = false;

  private File baseDir = new File("/tmp");

  private String filenamePattern = "{0}-{1}.pdf";

  boolean verbose = false;

  protected String password_;

  protected String username_;

  boolean quiet;

  public String getUsername() {
    return username_;
  }

  public String getPassword() {
    return password_;
  }

  public void setPassword(final String password) {
    password_ = password;
  }

  public void setUsername(final String username) {
    username_ = username;
  }

  public File getFile(final Date date, final String accountNumber) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    return new File(baseDir, MessageFormat.format(filenamePattern, accountNumber, dateFormat.format(date)));
  }

  public void verboseLog(final String message) {
    if (verbose) {
      System.err.println(message);
    }
  }

  public void log(final String message) {
    if (!quiet) {
      System.out.println(message);
    }
  }

  public File getSaveFile(final Date date, final String accountNumber) {
      File savefile = getFile(date, accountNumber);

      if (savefile.exists() && !overwrite) {
        log("File exists, skipping. " + savefile);
        return null;
      }
      else {
        log("Saving " + savefile);
      }
      return savefile;
    }
}
