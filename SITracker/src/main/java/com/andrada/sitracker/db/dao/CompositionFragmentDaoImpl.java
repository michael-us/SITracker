package com.andrada.sitracker.db.dao;

import com.andrada.sitracker.db.beans.CompositionFragment;
import com.j256.ormlite.dao.BaseDaoImpl;
import com.j256.ormlite.support.ConnectionSource;

import java.sql.SQLException;

public class CompositionFragmentDaoImpl extends BaseDaoImpl<CompositionFragment, Long>
        implements CompositionFragmentDao {
    public CompositionFragmentDaoImpl(ConnectionSource connectionSource) throws SQLException {
        super(connectionSource, CompositionFragment.class);
    }
}
