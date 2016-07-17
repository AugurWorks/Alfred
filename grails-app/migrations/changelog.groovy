databaseChangeLog = {

    changeSet(author: "TheConnMan (generated)", id: "1468770653424-1") {
        createTable(tableName: "training_run") {
            column(autoIncrement: "true", name: "id", type: "BIGINT") {
                constraints(primaryKey: "true", primaryKeyName: "training_runPK")
            }

            column(name: "version", type: "BIGINT") {
                constraints(nullable: "false")
            }

            column(name: "data_sets", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "date_created", type: "datetime") {
                constraints(nullable: "false")
            }

            column(name: "learning_constant", type: "DOUBLE precision") {
                constraints(nullable: "false")
            }

            column(name: "net_id", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "rms_error", type: "DOUBLE precision") {
                constraints(nullable: "false")
            }

            column(name: "rounds_trained", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "row_count", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "seconds_elapsed", type: "INT") {
                constraints(nullable: "false")
            }

            column(name: "training_stage", type: "VARCHAR(255)") {
                constraints(nullable: "false")
            }

            column(name: "training_stop_reason", type: "VARCHAR(255)")
        }
    }
}
