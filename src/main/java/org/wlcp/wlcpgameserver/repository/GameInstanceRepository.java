package org.wlcp.wlcpgameserver.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.wlcp.wlcpgameserver.datamodel.master.GameInstance;

@Repository
public interface GameInstanceRepository extends JpaRepository<GameInstance, Integer> {
	List<GameInstance> findByUsernameIdAndDebugInstance(String usernameId, Boolean debugInstance);
}
