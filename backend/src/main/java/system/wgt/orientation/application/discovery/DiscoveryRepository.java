package system.wgt.orientation.application.discovery;

import system.wgt.orientation.domain.discovery.DiscoveryCollection;
import system.wgt.orientation.domain.discovery.DiscoveryCollectionSummary;

import java.util.List;
import java.util.Optional;

public interface DiscoveryRepository {
    StoreResult storeIfAbsent(DiscoveryCollection collection);

    List<DiscoveryCollectionSummary> listCollections();

    Optional<DiscoveryCollection> findById(String collectionId);

    long countCollections();

    record StoreResult(boolean created, DiscoveryCollection collection) {
    }
}
