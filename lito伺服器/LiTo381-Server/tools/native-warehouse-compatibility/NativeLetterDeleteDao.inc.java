    @Override public RepositoryLetterMutation deleteLetter(
            long characterObjectId, RepositoryLetter expected) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryLetterMutation.readOnly(expected);
        }
        if (expected == null) throw new IllegalArgumentException(
                "an expected physical letter is required");
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_items_time WRITE, character_pets WRITE, "
                    + "character_letter WRITE, lito_item_origin WRITE");
            try {
                RepositoryLetter current = loadLetter(
                        connection, characterObjectId, expected.itemObjectId());
                if (current == null) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.NOT_FOUND, null);
                if (!current.equals(expected)) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.CONFLICT, current);
                boolean bodyDeleted = false;
                boolean backingDeleted = false;
                try {
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM character_letter WHERE item_object_id=?")) {
                        delete.setLong(1, current.itemObjectId());
                        if (delete.executeUpdate() != 1) return new RepositoryLetterMutation(
                                RepositoryLetterMutationStatus.CONFLICT, current);
                        bodyDeleted = true;
                    }
                    // Preserve auxiliary rows until both authoritative core rows
                    // have gone. A known item-delete refusal can restore the exact
                    // validated body while its original owned backing still exists.
                    try (PreparedStatement delete = connection.prepareStatement(
                            "DELETE FROM character_items WHERE id=? AND char_id=? AND item_id=?")) {
                        delete.setLong(1, current.itemObjectId());
                        delete.setLong(2, characterObjectId);
                        delete.setLong(3, current.opened()
                                ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED);
                        if (delete.executeUpdate() != 1) throw new SQLException(
                                "physical-letter backing changed after body deletion");
                        backingDeleted = true;
                    }
                } catch (SQLException failure) {
                    if (!backingDeleted) {
                        if (bodyDeleted) try {
                            restoreDeletedPhysicalLetterBody(connection, characterObjectId, current);
                        } catch (SQLException restoreFailure) {
                            failure.addSuppressed(restoreFailure);
                        }
                        throw failure;
                    }
                    // executeUpdate proved both core deletions. A later close
                    // failure cannot turn durable deletion into a rollback.
                    System.out.println("[LiTo] deleted physical-letter core statement close failed object="
                            + current.itemObjectId() + ": " + failure.getMessage());
                }
                cleanupDeletedPhysicalLetterMetadata(connection, characterObjectId, current.itemObjectId());
                return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.SUCCESS, current);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static void restoreDeletedPhysicalLetterBody(
            Connection connection, long characterObjectId, RepositoryLetter letter) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT 1 FROM character_items WHERE id=? AND char_id=? AND item_id=? LIMIT 1")) {
            query.setLong(1, letter.itemObjectId());query.setLong(2, characterObjectId);
            query.setLong(3, letter.opened() ? InventoryCatalog.LETTER_OPEN : InventoryCatalog.LETTER_CLOSED);
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) throw new SQLException("cannot restore letter body without original owned backing");
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO character_letter (item_object_id,code,sender,receiver,date,"
                        + "template_id,subject,content,isCheck) VALUES (?,?,?,?,?,?,?,?,?)")) {
            insert.setLong(1, letter.itemObjectId());insert.setInt(2, letter.code());
            insert.setString(3, letter.sender());insert.setString(4, letter.receiver());
            insert.setString(5, letter.date());insert.setInt(6, letter.templateId());
            insert.setBytes(7, terminatedUtf16Le(letter.subject()));
            insert.setBytes(8, terminatedUtf16Le(letter.content()));insert.setInt(9, letter.opened() ? 1 : 0);
            if (insert.executeUpdate() != 1) throw new SQLException("validated physical-letter body restoration failed");
        }
    }

    // Both core rows are already deleted. Failed auxiliary cleanup remains an
    // orphan to reconcile; attempt every cleanup and preserve durable SUCCESS.
    private static void cleanupDeletedPhysicalLetterMetadata(
            Connection connection, long characterObjectId, long itemObjectId) {
        for (String sql : new String[] {
                "DELETE FROM character_pets WHERE item_obj_id=?",
                "DELETE FROM character_items_time WHERE itemr_obj_id=?",
                "DELETE FROM lito_item_origin WHERE item_obj_id=? AND char_obj_id=?" }) {
            try (PreparedStatement cleanup = connection.prepareStatement(sql)) {
                cleanup.setLong(1, itemObjectId);
                if (sql.contains("char_obj_id")) cleanup.setLong(2, characterObjectId);
                cleanup.executeUpdate();
            } catch (SQLException failure) {
                System.out.println("[LiTo] deleted physical-letter metadata cleanup failed object="
                        + itemObjectId + ": " + failure.getMessage());
            }
        }
    }

