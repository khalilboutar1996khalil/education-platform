-- Class codes were meant to gate self-registration, but registration never asked for one: the
-- student picks their level from the admin-managed list (school_levels). The table and its admin
-- screen were unused, so they go. V15 and V16 stay as they are, since Flyway checks their checksums.

DROP TABLE class_codes;
