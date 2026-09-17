'use client';

import { MainPage } from '../../views/MainPage';
import { RouteStateSync } from '../RouteStateSync';

export default function Page() {
  return (
    <>
      <RouteStateSync page="main" />
      <MainPage />
    </>
  );
}
