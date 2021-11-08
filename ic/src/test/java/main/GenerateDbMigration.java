package main;

import io.ebean.annotation.Platform;
import io.ebean.dbmigration.DbMigration;

import java.io.IOException;

/**
 * Generate the DB Migration.
 */
public class GenerateDbMigration {

  // Note: working directory must be `ic` when you run this
  public static void main(String[] args) throws Exception {
    System.setProperty("ddl.migration.version", "27.0");
    System.setProperty("ddl.migration.name", "Widen narrow varchar columns");

    DbMigration dbMigration = DbMigration.create();
    dbMigration.setPlatform(Platform.POSTGRES);

    // generate the migration ddl and xml
    dbMigration.generateMigration();
  }
}
