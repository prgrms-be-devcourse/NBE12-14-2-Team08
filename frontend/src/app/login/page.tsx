'use client';

import { AuthPage } from '../../views/AuthPage';

export default async function Page({
  searchParams,
}: {
  searchParams: Promise<{ redirect?: string | string[] }>;
}) {
  const params = await searchParams;
  const requestedRedirect = Array.isArray(params.redirect) ? params.redirect[0] : params.redirect;
  const redirectTo = requestedRedirect?.startsWith('/') && !requestedRedirect.startsWith('//')
    ? requestedRedirect
    : '/main';

  return <AuthPage initialMode="login" redirectTo={redirectTo} />;
}
