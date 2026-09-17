'use client';

import { AuthPage } from '../../views/AuthPage';
import { RouteStateSync } from '../RouteStateSync';

export default function Page() {
  return (
    <>
      <RouteStateSync page="signup" />
      <AuthPage initialMode="signup" />
    </>
  );
}
