export type User = {
  id: string;
  full_name: string;
  email: string;
  role: "student" | "instructor" | "admin" | "coordinator";
  cohort_id: string | null;
};

export type Session = {
  id: string;
  cohort_id: string;
  instructor_id: string;
  classroom: string;
  start_at: string;
  end_at: string;
  status: "scheduled" | "active" | "closed";
};

export type AttendanceRecord = {
  id: string;
  session_id: string;
  user_id: string;
  self_mark_at: string | null;
  ble_first_detected_at: string | null;
  ble_last_detected_at: string | null;
  ble_detected_throughout: boolean;
  instructor_called_absent: boolean;
  instructor_override: string | null;
  final_flag: "Present" | "Absent" | "PresentButNotAtLecture" | "PresentButLeftInBetween" | null;
  finalised_at: string | null;
};

export type AuditEntry = {
  id: string;
  actor_id: string | null;
  action: string;
  target_table: string;
  target_id: string | null;
  payload: Record<string, unknown> | null;
  created_at: string;
};
