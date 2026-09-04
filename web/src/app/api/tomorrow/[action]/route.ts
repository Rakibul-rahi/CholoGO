import { NextRequest } from "next/server";

const API_BASE_URL = "https://chologo.onrender.com";

const ALLOWED_ACTIONS = new Set([
  "cancel-request",
  "notify-accepted",
  "notify-match",
]);

/**
 * Server-side proxy to the standalone Tomorrow-ride REST server. Some
 * transitions (a passenger cancelling an already-accepted request) are
 * deliberately not permitted by firestore.rules directly — only this
 * server's Admin SDK can finalize them, same as the Android app. The
 * server itself has no CORS headers (it only ever expected the Android
 * app as a caller), so this route exists to make the request from Next's
 * server runtime instead of the browser.
 */
export async function POST(
  request: NextRequest,
  context: { params: Promise<{ action: string }> }
) {
  const { action } = await context.params;

  if (!ALLOWED_ACTIONS.has(action)) {
    return Response.json({ error: "Unknown action." }, { status: 404 });
  }

  const authorization = request.headers.get("authorization") ?? "";
  const body = await request.text();

  const upstream = await fetch(`${API_BASE_URL}/api/tomorrow/${action}`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: authorization,
    },
    body,
  });

  const responseBody = await upstream.text();

  return new Response(responseBody, {
    status: upstream.status,
    headers: { "Content-Type": "application/json" },
  });
}
