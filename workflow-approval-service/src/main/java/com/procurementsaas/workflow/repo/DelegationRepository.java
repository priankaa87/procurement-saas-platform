package com.procurementsaas.workflow.repo;

import com.procurementsaas.workflow.domain.Delegation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DelegationRepository extends JpaRepository<Delegation, Long> {

    /** Every unrevoked grant of a role to this user; validity by date is checked in code. */
    List<Delegation> findByToUserAndRoleCodeAndRevokedFalse(String toUser, String roleCode);

    List<Delegation> findByFromUserOrderByValidFromDesc(String fromUser);
    List<Delegation> findByToUserOrderByValidFromDesc(String toUser);
}
