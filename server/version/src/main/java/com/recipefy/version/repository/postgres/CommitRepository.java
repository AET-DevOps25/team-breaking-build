package com.recipefy.version.repository.postgres;

import com.recipefy.version.model.postgres.Commit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommitRepository extends JpaRepository<Commit, Long> {

}

