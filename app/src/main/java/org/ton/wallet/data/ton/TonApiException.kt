package org.ton.wallet.data.ton

import drinkless.org.ton.TonApi

class TonApiException(val error: TonApi.Error) : RuntimeException()