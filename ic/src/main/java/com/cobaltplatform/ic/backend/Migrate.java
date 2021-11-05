package com.cobaltplatform.ic.backend;

import io.ebean.Database;
import io.ebean.DatabaseFactory;
import io.ebean.config.DatabaseConfig;
import io.ebean.migration.MigrationConfig;
import io.ebean.migration.MigrationRunner;

import java.util.Locale;
import java.util.TimeZone;

public class Migrate {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        Locale.setDefault(Locale.US);

        DatabaseConfig config = new DatabaseConfig();
        config.loadFromProperties();
        Database db = DatabaseFactory.create(config);

        MigrationConfig migrationConfig = new MigrationConfig();
        migrationConfig.load(config.getProperties());

        MigrationRunner runner = new MigrationRunner(migrationConfig);
        runner.run(db.getDataSource());
        db.shutdown();
    }
}
