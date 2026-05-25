import L from 'leaflet';
import { MapContainer, Marker, TileLayer } from 'react-leaflet';
import iconRetina from 'leaflet/dist/images/marker-icon-2x.png';
import icon from 'leaflet/dist/images/marker-icon.png';
import shadow from 'leaflet/dist/images/marker-shadow.png';
import { Card, CardContent } from '../ui/card';

L.Icon.Default.mergeOptions({ iconRetinaUrl: iconRetina, iconUrl: icon, shadowUrl: shadow });

const amberIcon = L.divIcon({
  className: '',
  html: '<div style="height:18px;width:18px;border-radius:9999px;background:#f59e0b;border:3px solid #fef3c7;box-shadow:0 0 0 6px rgba(245,158,11,0.25);"></div>',
  iconSize: [18, 18],
  iconAnchor: [9, 9]
});

interface EndpointMapProps {
  latitude: number | null;
  longitude: number | null;
  city: string | null;
}

export function EndpointMap({ latitude, longitude, city }: EndpointMapProps) {
  if (latitude === null || longitude === null) {
    return (
      <Card>
        <CardContent className="flex h-[200px] items-center justify-center text-sm text-slate-400 md:h-[300px]">
          Location unavailable
        </CardContent>
      </Card>
    );
  }

  return (
    <div className="h-[200px] overflow-hidden rounded-lg border border-slate-800 md:h-[300px]">
      <MapContainer center={[latitude, longitude]} zoom={city ? 10 : 6} scrollWheelZoom={false} className="h-full w-full">
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> &copy; <a href="https://carto.com/attributions">CARTO</a>'
          url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
        />
        <Marker position={[latitude, longitude]} icon={amberIcon} />
      </MapContainer>
    </div>
  );
}
