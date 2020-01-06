package com.andrada.sitracker.db.beans;

import com.andrada.sitracker.db.dao.CompositionFragmentDaoImpl;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;

import java.io.Serializable;

@DatabaseTable(daoClass = CompositionFragmentDaoImpl.class, tableName = "composition_fragments")
@SuppressWarnings("WeakerAccess")
public class CompositionFragment implements Serializable {
    private static final long serialVersionUID = 2233514255956207628L;

    @DatabaseField(generatedId = true, useGetSet = true)
    long id;
    @DatabaseField(canBeNull = false,
            foreign = true,
            foreignAutoRefresh = true,
            indexName = "composition_id_idx",
            columnDefinition = "bigint references compositions(id)")
    Composition composition;
    @DatabaseField(canBeNull = false,
            foreign = true,
            foreignAutoRefresh = true,
            indexName = "publication_id_idx",
            columnDefinition = "bigint references publications(id)")
    Publication publication;
    @DatabaseField(useGetSet = true)
    int order;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Composition getComposition() {
        return composition;
    }

    public void setComposition(Composition composition) {
        this.composition = composition;
    }

    public Publication getPublication() {
        return publication;
    }

    public void setPublication(Publication publication) {
        this.publication = publication;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
