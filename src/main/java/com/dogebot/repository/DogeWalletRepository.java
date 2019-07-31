package com.dogebot.repository;

import com.dogebot.domain.DogeWallet;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DogeWalletRepository extends CrudRepository<DogeWallet, Long> {
  List<DogeWallet> findByTwitterUserId(String id);
}
