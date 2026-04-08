package com.generic4.itda.repository;

import com.generic4.itda.domain.file.StoredFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoredFileRepository extends JpaRepository<StoredFile, Long> {

}
