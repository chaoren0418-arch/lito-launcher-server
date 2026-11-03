    @Override public RepositoryLetterMutation openLetter(
            long characterObjectId, RepositoryLetter expected) throws SQLException {
        if (!repositoryMode.equals("lito_mysql") || readOnly) {
            return RepositoryLetterMutation.readOnly(expected);
        }
        if (expected == null || expected.opened()) {
            throw new IllegalArgumentException("an expected closed physical letter is required");
        }
        try (Connection connection = connection(accountUrl, accountLogin, accountPassword);
             Statement lock = connection.createStatement()) {
            lock.execute("LOCK TABLES characters READ, character_items WRITE, "
                    + "character_letter WRITE, lito_item_origin WRITE");
            try {
                RepositoryLetter current = loadLetter(
                        connection, characterObjectId, expected.itemObjectId());
                if (current == null) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.NOT_FOUND, null);
                if (!sameLetterBody(current, expected)) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.CONFLICT, current);
                if (current.opened()) return new RepositoryLetterMutation(
                        RepositoryLetterMutationStatus.ALREADY_OPEN, current);
                boolean backingOpened = false;
                boolean bodyOpened = false;
                try {
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE character_items SET item_id=? WHERE id=? AND char_id=? "
                                    + "AND item_id=?")) {
                        update.setLong(1, InventoryCatalog.LETTER_OPEN);
                        update.setLong(2, current.itemObjectId());
                        update.setLong(3, characterObjectId);
                        update.setLong(4, InventoryCatalog.LETTER_CLOSED);
                        if (update.executeUpdate() != 1) return new RepositoryLetterMutation(
                                RepositoryLetterMutationStatus.CONFLICT, current);
                        backingOpened = true;
                    }
                    try (PreparedStatement update = connection.prepareStatement(
                            "UPDATE character_letter SET isCheck=1 WHERE item_object_id=? "
                                    + "AND isCheck=0")) {
                        update.setLong(1, current.itemObjectId());
                        if (update.executeUpdate() != 1) throw new SQLException(
                                "physical-letter body changed after backing opened");
                        bodyOpened = true;
                    }
                } catch (SQLException failure) {
                    if (!bodyOpened) {
                        if (backingOpened) try {
                            restoreClosedPhysicalLetterBacking(connection, characterObjectId, current);
                        } catch (SQLException restoreFailure) {
                            failure.addSuppressed(restoreFailure);
                        }
                        throw failure;
                    }
                    // Both core updates are proven. Statement close failure
                    // cannot turn a durable open into a closed-letter rollback.
                    System.out.println("[LiTo] opened physical-letter core statement close failed object="
                            + current.itemObjectId() + ": " + failure.getMessage());
                }
                refreshOpenedPhysicalLetterOrigin(connection, characterObjectId, current.itemObjectId());
                RepositoryLetter opened = new RepositoryLetter(current.itemObjectId(), current.code(),
                        current.sender(), current.receiver(), current.date(), current.templateId(),
                        current.subject(), current.content(), true);
                return new RepositoryLetterMutation(RepositoryLetterMutationStatus.SUCCESS, opened);
            } finally {
                lock.execute("UNLOCK TABLES");
            }
        }
    }

    private static void restoreClosedPhysicalLetterBacking(
            Connection connection, long characterObjectId, RepositoryLetter letter) throws SQLException {
        // Do not close an item against a body that is already opened. A lost
        // execution reply remains an ambiguous failure rather than guessed rollback.
        try (PreparedStatement query = connection.prepareStatement(
                "SELECT isCheck FROM character_letter WHERE item_object_id=? LIMIT 1")) {
            query.setLong(1, letter.itemObjectId());
            try (ResultSet row = query.executeQuery()) {
                if (!row.next() || row.getInt(1) != 0) throw new SQLException(
                        "cannot close backing without original closed letter body");
            }
        }
        try (PreparedStatement restore = connection.prepareStatement(
                "UPDATE character_items SET item_id=? WHERE id=? AND char_id=? AND item_id=?")) {
            restore.setLong(1, InventoryCatalog.LETTER_CLOSED);
            restore.setLong(2, letter.itemObjectId());restore.setLong(3, characterObjectId);
            restore.setLong(4, InventoryCatalog.LETTER_OPEN);
            if (restore.executeUpdate() != 1) throw new SQLException(
                    "validated physical-letter backing restoration failed");
        }
    }

    private static void refreshOpenedPhysicalLetterOrigin(
            Connection connection, long characterObjectId, long itemObjectId) {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE lito_item_origin SET catalog_id=? WHERE item_obj_id=? AND char_obj_id=?")) {
            update.setLong(1, InventoryCatalog.LETTER_OPEN);
            update.setLong(2, itemObjectId);update.setLong(3, characterObjectId);
            update.executeUpdate();
        } catch (SQLException failure) {
            System.out.println("[LiTo] opened physical-letter origin refresh failed object="
                    + itemObjectId + ": " + failure.getMessage());
        }
    }

