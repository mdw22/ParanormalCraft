/**
 * Client-only code. {@link io.github.mdw22.paranormalcraft.client.ParanormalCraftClient} is the entry point (init, the
 * client tick, entity renderers); {@code ClientToolState} shows EMF and thermometer readings. Nothing on a server code path
 * may reference this package or its subpackages; common code reaches the client through {@code network.ModNetwork}'s
 * client handlers or {@code debug.trace.ClientTraceHook}.
 */
package io.github.mdw22.paranormalcraft.client;
