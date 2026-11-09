package com.nexus.rinde.iam.application.internal.queryservices;

import com.nexus.rinde.iam.domain.model.aggregates.User;
import com.nexus.rinde.iam.domain.model.queries.GetUserByIdQuery;
import com.nexus.rinde.iam.domain.model.queries.GetUsersByTenantQuery;
import com.nexus.rinde.iam.domain.services.UserQueryService;
import com.nexus.rinde.iam.infrastructure.persistence.jpa.repositories.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consulta usuarios siempre dentro de una empresa; nunca devuelve los de otra. */
@Service
public class UserQueryServiceImpl implements UserQueryService {

  private final UserRepository userRepository;

  public UserQueryServiceImpl(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<User> handle(GetUsersByTenantQuery query) {
    return userRepository.findByTenantId(query.tenantId()).stream()
        .filter(user -> query.role() == null || user.getRole() == query.role())
        .sorted(Comparator.comparing(user -> user.getFullName().toLowerCase()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<User> handle(GetUserByIdQuery query) {
    return userRepository
        .findById(query.userId().value())
        .filter(user -> user.belongsTo(query.tenantId()));
  }
}
