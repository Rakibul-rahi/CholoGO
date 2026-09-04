"use client";

import { useEffect, useState } from "react";
import { useAuth } from "@/context/AuthContext";
import { listenTotalXp } from "@/lib/xp";
import LevelCard from "@/components/dashboard/LevelCard";
import RideTabs from "@/components/dashboard/RideTabs";
import PassengerRideNow from "@/components/dashboard/ridenow/PassengerRideNow";
import RiderRideNow from "@/components/dashboard/ridenow/RiderRideNow";
import PassengerTomorrow from "@/components/dashboard/tomorrow/PassengerTomorrow";
import RiderTomorrow from "@/components/dashboard/tomorrow/RiderTomorrow";

export default function DashboardHomePage() {
  const { profile } = useAuth();
  const [xp, setXp] = useState(0);
  const [xpLoading, setXpLoading] = useState(true);

  useEffect(() => {
    if (!profile?.uid) return;
    setXpLoading(true);
    return listenTotalXp(profile.uid, (total) => {
      setXp(total);
      setXpLoading(false);
    });
  }, [profile?.uid]);

  if (!profile) return null;

  const isRider = profile.role === "rider";

  return (
    <div>
      <h1 className="text-2xl font-semibold tracking-tight">
        {isRider ? "Rider dashboard" : "Passenger dashboard"}
      </h1>

      <div className="mt-6">
        <LevelCard xp={xp} userName={profile.name} loading={xpLoading} />
      </div>

      <RideTabs
        rideNow={
          isRider ? (
            <RiderRideNow
              riderId={profile.uid}
              riderName={profile.name}
              riderPhone={profile.phone}
              vehicleType={profile.vehicleType}
              vehicleModel={profile.vehicleModel}
              vehicleNumber={profile.vehicleNumber}
              vehicleColor={profile.vehicleColor}
            />
          ) : (
            <PassengerRideNow
              passengerId={profile.uid}
              passengerName={profile.name}
              passengerPhone={profile.phone}
            />
          )
        }
        tomorrow={
          isRider ? (
            <RiderTomorrow
              riderId={profile.uid}
              riderName={profile.name}
              riderPhone={profile.phone}
              vehicleType={profile.vehicleType}
              vehicleModel={profile.vehicleModel}
              vehicleNumber={profile.vehicleNumber}
              vehicleColor={profile.vehicleColor}
            />
          ) : (
            <PassengerTomorrow
              passengerId={profile.uid}
              passengerName={profile.name}
              passengerPhone={profile.phone}
            />
          )
        }
      />
    </div>
  );
}
