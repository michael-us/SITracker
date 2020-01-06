package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.Composition;
import com.j256.ormlite.dao.Dao;

import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.List;

public interface CompositionDao extends Dao<Composition, Long> {
    @NotNull
    List<Composition> getCompositionsForAuthorId(long authorId) throws SQLException;
}
