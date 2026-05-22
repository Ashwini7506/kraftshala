import "./globals.css";
import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Kraftshala Attendance — Admin",
  description: "Dashboard for offline attendance system"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
