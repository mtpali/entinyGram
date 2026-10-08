package desu.inugram

import org.telegram.messenger.ApplicationLoaderImpl as BaseApplicationLoaderImpl
import org.telegram.messenger.GoogleMapsProvider
import org.telegram.messenger.IMapsProvider
import desu.inugram.helpers.maps.MapsHelper

class ApplicationLoaderImpl : BaseApplicationLoaderImpl() {
    override fun isStandalone(): Boolean = false

    override fun onCreateMapsProvider(): IMapsProvider? {
        if (InuConfig.MAP_PROVIDER.value == InuConfig.MapProviderItem.OSM_LITE && MapsHelper.hasOsmdroid) {
            return MapsHelper.newOsmdroidProvider()
        }
        return GoogleMapsProvider()
    }
}
