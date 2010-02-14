package com.bitclean.billscrape;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import com.bitclean.billscrape.eonenergy.EonScraper;
import com.bitclean.billscrape.virginmedia.VirginScraper;
import com.bitclean.billscrape.Tmobile.TmobileScraper;
import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.esotericsoftware.yamlbeans.scalar.ScalarSerializer;

public class BillDownload {
  public static void main(String[] args) throws FileNotFoundException, YamlException {
    String filename = "/tmp/config.yaml";
    if (args.length > 0) {
      filename = args[0];
    }
    YamlReader reader = new YamlReader(new FileReader(filename));
    
    reader.getConfig().setPrivateFields(true);
    reader.getConfig().setScalarSerializer(File.class, new ScalarSerializer<File>() {

      public String write(final File file) throws YamlException {
        return file.toString();
      }

      public File read(final String s) throws YamlException {
        return new File(s);
      }
    });
    reader.getConfig().setClassTag("virgin", VirginScraper.Options.class);
    reader.getConfig().setClassTag("eonenergy", EonScraper.Options.class);
    reader.getConfig().setClassTag("tmobile", TmobileScraper.Options.class);
    
    while(true) {
      ScraperDefinition def = (ScraperDefinition) reader.read();
      if (def == null) break;
      
      def.getInstance().run();
    }
    
  }
}
