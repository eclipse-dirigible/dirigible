package org.eclipse.dirigible.components.tracing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * The Interface TaskStateRepository.
 */
@Repository("taskStateRepository")
public interface TaskStateRepository extends JpaRepository<TaskState, Long> {

}
