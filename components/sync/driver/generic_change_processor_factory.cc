// Copyright 2014 The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

#include "components/sync/driver/generic_change_processor_factory.h"

#include <utility>

#include "base/memory/ptr_util.h"
#include "components/sync/api/syncable_service.h"
#include "components/sync/driver/generic_change_processor.h"

namespace syncer {

GenericChangeProcessorFactory::GenericChangeProcessorFactory() {}

GenericChangeProcessorFactory::~GenericChangeProcessorFactory() {}

std::unique_ptr<GenericChangeProcessor>
GenericChangeProcessorFactory::CreateGenericChangeProcessor(
    ModelType type,
    UserShare* user_share,
    std::unique_ptr<DataTypeErrorHandler> error_handler,
    const base::WeakPtr<SyncableService>& local_service,
    const base::WeakPtr<SyncMergeResult>& merge_result,
    SyncClient* sync_client) {
  DCHECK(user_share);
  return base::MakeUnique<GenericChangeProcessor>(
      type, std::move(error_handler), local_service, merge_result, user_share,
      sync_client, local_service->GetAttachmentStoreForSync());
}

}  // namespace syncer
