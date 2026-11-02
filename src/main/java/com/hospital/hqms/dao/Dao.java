package com.hospital.hqms.dao;

import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object contract.
 *
 * @param <T> the entity type handled by the DAO
 */
public interface Dao<T> {

    Optional<T> findById(int id);

    List<T> findAll();
}
