export interface Badge {
  code: string;
  title: string;
  description: string;
  earned: boolean;
}

export interface GamificationStats {
  totalFoodLogs: number;
  streakDays: number;
  metWaterTargetToday: boolean;
  metProteinTargetToday: boolean;
  weightLostKg: number; // positive = lost since first check-in
  checkinCount: number;
}

/** Deterministic badge evaluation from a user's activity stats. Pure. */
export function computeBadges(s: GamificationStats): Badge[] {
  const b = (code: string, title: string, description: string, earned: boolean): Badge => ({
    code,
    title,
    description,
    earned,
  });

  return [
    b('first_log', 'First Bite', 'Logged your first food entry', s.totalFoodLogs >= 1),
    b('streak_3', '3-Day Streak', 'Logged food 3 days in a row', s.streakDays >= 3),
    b('streak_7', 'Week Warrior', 'Logged food 7 days in a row', s.streakDays >= 7),
    b('streak_30', 'Consistency King', '30-day logging streak', s.streakDays >= 30),
    b('hydrated', 'Hydrated', 'Hit your water target for the day', s.metWaterTargetToday),
    b('protein_pro', 'Protein Pro', 'Hit your protein target for the day', s.metProteinTargetToday),
    b('checkin_starter', 'Tracking Progress', 'Recorded your first weekly check-in', s.checkinCount >= 1),
    b('down_1kg', 'First Kilo', 'Lost your first kilogram', s.weightLostKg >= 1),
    b('down_5kg', 'Five Down', 'Lost 5 kg from your starting weight', s.weightLostKg >= 5),
  ];
}

export function badgeSummary(badges: Badge[]) {
  const earned = badges.filter((x) => x.earned);
  return { earnedCount: earned.length, total: badges.length, badges };
}
