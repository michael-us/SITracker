package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Composition;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.support.ConnectionSource;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CompositionDaoImpl extends BaseDaoImpl<Composition, Long>
        implements CompositionDao {
    public CompositionDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, Composition.class);
    }

    @NotNull
    @Override
    public List<Composition> getCompositionsForAuthorId(long authorId) throws SQLException {
        final GenericRawResults<String[]> results = this.queryRaw(
                "SELECT DISTINCT c.id, c.name FROM compositions AS c" +
                " INNER JOIN composition_fragments AS f ON f.composition_id = c.id" +
                " INNER JOIN publications AS p ON p.id = f.publication_id" +
                " WHERE p.author_id = ?", String.valueOf(authorId));
        final List<Composition> compositions = new ArrayList<>();
        final Iterator<String[]> iterator = results.iterator();
        while (iterator.hasNext()) {
            final Composition composition = new Composition();
            final String[] values = iterator.next();
            composition.setId(Long.valueOf(values[0]));
            composition.setName(values[1]);
            compositions.add(composition);
        }
        return compositions;
    }
}
