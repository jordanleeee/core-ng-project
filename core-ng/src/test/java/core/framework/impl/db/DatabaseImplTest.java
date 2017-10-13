package core.framework.impl.db;

import core.framework.db.Transaction;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author neo
 */
public class DatabaseImplTest {
    private static DatabaseImpl database;

    @BeforeClass
    public static void createDatabase() {
        database = new DatabaseImpl();
        database.url("jdbc:hsqldb:mem:.;sql.syntax_mys=true");
        database.view(EntityView.class);

        database.execute("CREATE TABLE database_test (id INT PRIMARY KEY, string_field VARCHAR(20), enum_field VARCHAR(10))");
    }

    @AfterClass
    public static void cleanupDatabase() {
        database.execute("DROP TABLE database_test");
    }

    @Before
    public void truncateTable() {
        database.execute("TRUNCATE TABLE database_test");
    }

    @Test
    public void selectOneWithView() {
        database.execute("INSERT INTO database_test VALUES (?, ?, ?)", 1, "string", TestEnum.V1);

        EntityView view = database.selectOne("SELECT string_field as string_label, enum_field as enum_label FROM database_test where id = ?", EntityView.class, 1).get();

        assertEquals("string", view.stringField);
        assertEquals(TestEnum.V1, view.enumField);
    }

    @Test
    public void selectWithView() {
        database.execute("INSERT INTO database_test VALUES (?, ?, ?)", 1, "string", TestEnum.V1);
        database.execute("INSERT INTO database_test VALUES (?, ?, ?)", 2, "string", TestEnum.V2);

        List<EntityView> views = database.select("SELECT string_field as string_label, enum_field as enum_label FROM database_test", EntityView.class);

        assertEquals(2, views.size());
        assertEquals(TestEnum.V1, views.get(0).enumField);
        assertEquals(TestEnum.V2, views.get(1).enumField);
    }

    @Test
    public void selectEmptyWithView() {
        List<EntityView> views = database.select("SELECT string_field, enum_field FROM database_test where id = -1", EntityView.class);

        assertTrue(views.isEmpty());
    }

    @Test
    public void selectNullInt() {
        Optional<Integer> result = database.selectOne("SELECT max(id) FROM database_test", Integer.class);
        assertFalse(result.isPresent());
    }

    @Test
    public void selectInt() {
        Optional<Integer> result = database.selectOne("SELECT count(id) FROM database_test", Integer.class);
        assertTrue(result.isPresent());
        assertEquals(0, result.get().intValue());
    }

    @Test
    public void commitTransaction() {
        try (Transaction transaction = database.beginTransaction()) {
            database.execute("INSERT INTO database_test VALUES (?, ?, ?)", 1, "string", TestEnum.V1);
            transaction.commit();
        }

        Optional<EntityView> result = database.selectOne("SELECT string_field, enum_field FROM database_test where id = ?", EntityView.class, 1);
        assertTrue(result.isPresent());
    }

    @Test
    public void rollbackTransaction() {
        try (Transaction transaction = database.beginTransaction()) {
            database.execute("INSERT INTO database_test VALUES (?, ?, ?)", 1, "string", TestEnum.V1);
            transaction.rollback();
        }

        Optional<EntityView> result = database.selectOne("SELECT string_field, enum_field FROM database_test where id = ?", EntityView.class, 1);
        assertFalse(result.isPresent());
    }
}
