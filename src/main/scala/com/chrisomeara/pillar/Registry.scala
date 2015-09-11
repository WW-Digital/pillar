package com.chrisomeara.pillar

import java.util.Date
import java.io.{FileInputStream, File}

import org.slf4j.LoggerFactory

object Registry {
  var log = LoggerFactory.getLogger(this.getClass)

  def apply(migrations: Seq[Migration]): Registry = {
    new Registry(migrations)
  }

  def fromDirectory(directory: File, reporter: Reporter): Registry = {
    new Registry(parseMigrationsInDirectory(directory).map(new ReportingMigration(reporter, _)))
  }

  def fromDirectory(directory: File): Registry = {
    new Registry(parseMigrationsInDirectory(directory))
  }

  private def parseMigrationsInDirectory(directory: File): Seq[Migration] = {
    if(!directory.isDirectory) {
      log.info(s"${directory.getAbsolutePath} is not a directory")
      return List.empty
    }

    val parser = Parser()

    directory.listFiles().map {
      val files = directory.listFiles()
      file =>
        val stream = new FileInputStream(file)
        try {
          log.info(s"Attempting to parse ${file.getAbsolutePath}")
          parser.parse(stream)
        } finally {
          stream.close()
        }
    }.toList
  }
}

class Registry(private var migrations: Seq[Migration]) {
  migrations = migrations.sortBy(_.authoredAt)

  private val migrationsByKey = migrations.foldLeft(Map.empty[MigrationKey, Migration]) {
    (memo, migration) => memo + (migration.key -> migration)
  }

  def authoredBefore(date: Date): Seq[Migration] = {
    migrations.filter(migration => migration.authoredBefore(date))
  }

  def apply(key: MigrationKey): Migration = {
    migrationsByKey(key)
  }

  def all: Seq[Migration] = migrations
}
