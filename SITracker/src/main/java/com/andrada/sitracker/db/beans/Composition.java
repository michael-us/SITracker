package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.CompositionDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(daoClass = CompositionDaoImpl.class, tableName = "compositions")
@SuppressWarnings("WeakerAccess")
public class Composition implements Serializable {
    private static final long serialVersionUID = 8235849844337362902L;

    @DatabaseField(generatedId = true, useGetSet = true)
    long id;
    @DatabaseField(canBeNull = false, useGetSet = true)
    String name;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
