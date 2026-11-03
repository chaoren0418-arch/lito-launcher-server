#!/usr/bin/env python3
"""Safely probe or compat-launch the authorized legacy LiTo client.

The target starts suspended, is attached to a kill-on-close Job object, and is
then resumed.  Process/module lifecycle data is recorded until the target exits
or the optional time limit is reached.  The compatibility mode installs a small
in-process exception handler and applies narrowly matched volatile memory
patches; it never edits the executable on disk or loads a legacy kernel driver.
"""

from __future__ import annotations

import sys
sys.path[:0]=['D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\tools\\world-resource-compat', 'D:\\lineage開發\\3-lito開發\\lito登入器\\LiTo381-Launcher\\app']
import argparse
import ctypes
import ctypes.wintypes as wt
import hashlib
import ipaddress
import json
import os
import struct
import subprocess
import time
from datetime import datetime
from pathlib import Path
from typing import Any

import pefile


CREATE_SUSPENDED = 0x00000004
DEBUG_ONLY_THIS_PROCESS = 0x00000002
CREATE_UNICODE_ENVIRONMENT = 0x00000400
CREATE_NO_WINDOW = 0x08000000
TH32CS_SNAPPROCESS = 0x00000002
TH32CS_SNAPTHREAD = 0x00000004
TH32CS_SNAPMODULE = 0x00000008
TH32CS_SNAPMODULE32 = 0x00000010
INVALID_HANDLE_VALUE = ctypes.c_void_p(-1).value
STILL_ACTIVE = 259
PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
PROCESS_ALL_ACCESS = 0x001F0FFF
THREAD_SUSPEND_RESUME = 0x0002
THREAD_GET_CONTEXT = 0x0008
THREAD_QUERY_INFORMATION = 0x0040
WOW64_CONTEXT_FULL = 0x00010007
JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE = 0x00002000
JOB_OBJECT_EXTENDED_LIMIT_INFORMATION_CLASS = 9
SEM_FAILCRITICALERRORS = 0x0001
SEM_NOGPFAULTERRORBOX = 0x0002
MEM_COMMIT = 0x1000
MEM_RESERVE = 0x2000
MEM_RELEASE = 0x8000
PAGE_EXECUTE_READWRITE = 0x40
WAIT_OBJECT_0 = 0
WAIT_TIMEOUT = 258
WM_MOUSEMOVE = 0x0200
WM_LBUTTONDOWN = 0x0201
WM_LBUTTONUP = 0x0202
MK_LBUTTON = 0x0001
EXCEPTION_DEBUG_EVENT = 1
EXIT_PROCESS_DEBUG_EVENT = 5
EXCEPTION_BREAKPOINT = 0x80000003
DBG_CONTINUE = 0x00010002
DBG_EXCEPTION_NOT_HANDLED = 0x80010001
CONNECT_IAT_SLOT = 0x004F74EC
CONNECT_ROUTINE = 0x004C62A0
CONNECT_ROUTINE_SIGNATURE = bytes.fromhex("55 8B EC 81 EC A8 01 00 00")
CONNECT_DIRECT_THUNK = 0x004EFE68
DESTROY_WINDOW_IAT_SLOT = 0x004F7484
POST_QUIT_MESSAGE_IAT_SLOT = 0x004F7430
POST_QUIT_MESSAGE_REFERENCE_CALL = 0x0047F44B
CLIENT_CHAR_SET = 0x004C3D30
CLIENT_CHAR_SET_SIGNATURE = bytes.fromhex("55 8B EC 56 57")
CLIENT_ACCOUNT_CELLS = 0x00641348
CLIENT_ACCOUNT_CELL_COUNT = 28
CLIENT_PASSWORD_CELLS = 0x006412A0
CLIENT_PASSWORD_CELL_COUNT = 14
CLIENT_CHARACTER_NAME = 0x0064AD54
CLIENT_CHARACTER_NAME_CAPACITY = 28
CLIENT_CHARACTER_DIRECTORY_RECORDS = 0x0062FAA0
CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE = 0xB4
CLIENT_CHARACTER_DIRECTORY_INDEX = 0x0076079C
CLIENT_CHARACTER_DIRECTORY_PENDING_COUNT = 0x007607A0
CLIENT_PACKET_WRITER = 0x004C6AE0
CLIENT_PACKET_WRITER_SIGNATURE = bytes.fromhex("55 8B EC B8 00 14 00 00")
# Both native login-send branches call the same packet writer after placing
# account/password pointers at identical stack offsets. The compatibility
# launcher hooks these call sites only in manual selection mode so the first
# physically submitted login can be retained inside the LiTo process for the
# subsequent LOGIN -> GAME reconnect.
CLIENT_NATIVE_LOGIN_CONNECTOR_SEND = 0x00492153
CLIENT_NATIVE_LOGIN_DIRECT_SEND = 0x00492172
CLIENT_INVENTORY_BATCH_RECEIVER = 0x004B2650
CLIENT_INVENTORY_BATCH_RECEIVER_SIGNATURE = bytes.fromhex(
    "55 8B EC 81 EC 2C 02 00 00 53 8B D9 43"
)
CLIENT_SERVER_DIRECTORY_RECEIVER = 0x004AC7D0
CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE = bytes.fromhex(
    "55 8B EC 81 EC 20 01 00 00 53"
)
CLIENT_PACKET_DISPATCHER = 0x004B2970
CLIENT_PACKET_DISPATCHER_SIGNATURE = bytes.fromhex(
    "55 8B EC 83 EC 54 A0 A2 AF 64 00"
)
CLIENT_SERVER_DIRECTORY_CLICK = 0x00475F40
CLIENT_SERVER_DIRECTORY_CLICK_SIGNATURE = bytes.fromhex(
    "55 8B EC 83 EC 14 53 56 8B F1 57"
)
CLIENT_ROOM_DIRECTORY_CLICK = 0x00475DA0
CLIENT_ROOM_DIRECTORY_CLICK_SIGNATURE = bytes.fromhex(
    "55 8B EC 83 EC 14 53 56 8B F1 57"
)
# sub_417970 releases the current image/input capture before a Tournament
# directory selection. The official launcher normally owns dword_64B8F8, but
# the verified standalone handoff can legitimately leave it null. Its second
# virtual call has no native null check, unlike the first optional owner.
CLIENT_TOURNAMENT_UI_FLUSH = 0x00417970
CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE = bytes.fromhex("8B 0D 48 6B 83 00")
CLIENT_TOURNAMENT_UI_MANAGER = 0x0064B8F8
CLIENT_ACTIVE_IMAGE_OWNER = 0x00836B48
CLIENT_IMAGE_CAPTURE_FLAG = 0x00640890
CLIENT_SERVER_DIRECTORY_ROOT = 0x0064B0A0
CLIENT_SERVER_DIRECTORY_CONSTRUCTOR = 0x00475070
CLIENT_SERVER_DIRECTORY_CONSTRUCTOR_SIGNATURE = bytes.fromhex(
    "55 8B EC 6A FF 68 6F 48 4F 00"
)
CLIENT_NATIVE_ALLOCATOR = 0x004F118C
CLIENT_FORMAT_CC = 0x00502778
CLIENT_FORMAT_CDS_POINTER = 0x00502CA8
TRANSFER_STONE_PROBE_ROOM_ID = 1
TRANSFER_STONE_PROBE_INVENTORY_PACKET = bytes.fromhex(
    "7C 01 01 00 00 6C 00 00 BB 4E 01 01 00 00 00 01 "
    "B6 C7 B0 65 A5 DB 00 00"
)
TRANSFER_STONE_PROBE_DIRECTORY_PACKET = (
    bytes.fromhex("FB 00 00 00 00 01 00 00 00")
    + b"LiTo 3.81C Transfer Stone Probe\x00"
    + bytes.fromhex("7F 00 00 02 64 7D 00 00 00 00 64 00 00 00")
)
# The protected Tournament parser keeps the selected local character in this
# separate fixed buffer.  The ordinary Lineage character-name cell above is
# not populated by the standalone VS auto-select path.
CLIENT_TOURNAMENT_CHARACTER_NAME = 0x0064ABCA
CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY = 28
# Tournament room handlers compare S_D0/S_D1 names against this separate
# local-player buffer.  The standalone auto-select path never fills it.
CLIENT_ROOM_LOCAL_NAME = 0x0064ABF8
CLIENT_ROOM_LOCAL_NAME_CAPACITY = 28
CLIENT_AUTO_LOGIN_FLAG = 0x0075F755
CLIENT_NATIVE_SOURCE_FORCE_SELECTOR_FLAG = 0x0075F756
CLIENT_VS_AUTO_SELECT_SENT_FLAG = 0x0064AFBE
CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG = 0x0064AF9D
# sub_4A7550 parses S_CB rows into the native character-selection UI only when
# both source-mode bytes are clear.  Either byte being set bypasses that UI and
# immediately emits C_C9 from CLIENT_CHARACTER_NAME instead.  That shortcut is
# correct for the compatibility automatic path, but in a manual launch without
# LITO381_CHARACTER the name cell is intentionally empty and the old shortcut
# emits the malformed packet `C9 00`.
CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A = 0x0064AFBC
CLIENT_CHARACTER_AUTO_SELECT_SOURCE_B = 0x0064AFBD
CLIENT_LITO_NEWS_HANDLER = 0x0048F500
CLIENT_LITO_NEWS_HANDLER_ENTRY = bytes.fromhex("A0 24 C2 62 00")
CLIENT_CHARACTER_PAGE_BUILDER = 0x00496EB0
CLIENT_CHARACTER_PAGE_BUILDER_SIGNATURE = bytes.fromhex(
    "8B 0D E8 F6 75 00 56 57 83 79 0C 05"
)
CLIENT_UI_PAGE_FACTORY = 0x00495520
CLIENT_UI_PAGE_REGISTRY = 0x00631E38
CLIENT_CHARACTER_PAGE_NETWORK_TAIL = 0x00496FB4
CLIENT_CHARACTER_PAGE_NETWORK_TAIL_SIGNATURE = bytes.fromhex(
    "A0 A0 AF 64 00 84 C0 74 09"
)
# The protected page-7 row callback normally highlights the row and then
# falls through to sub_4928C0, so every row click immediately emits C_C9 and
# replaces the list with the loading page.  The standalone launcher needs a
# conventional selector: rows update the native preview, while the existing
# left Confirm control remains the sole C_C9 producer.  Changing this one
# verified conditional branch to an unconditional return preserves all four
# original row callbacks and their selection/highlight work.
CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH = 0x004934E2
CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH_BYTES = bytes.fromhex("74 0A")
CLIENT_CHARACTER_ROW_LOCAL_ONLY_BRANCH_BYTES = bytes.fromhex("EB 0A")
# S_03 announces how many S_CB rows belong to the next character directory.
# The native handler stores that value only while parsing and clears it after
# the last row, so retain a process-private copy for the source-A handoff.  If
# source A is armed after merely the first row, sub_4A7550 skips all remaining
# rows and the page incorrectly leaves those slots as "New Character".
CLIENT_CHARACTER_COUNT_HANDLER = 0x004A7450
CLIENT_CHARACTER_COUNT_HANDLER_ENTRY = bytes.fromhex("55 8B EC 51 8D 45 FF")
# After the final S_CB row, sub_4A7550 tail-jumps through sub_497010.  That
# path calls sub_4934F0 before sub_497150 assigns the four row model resources,
# so the initial selector still paints "New Character" until a row click calls
# sub_4934F0 again.  Preserve the complete native tail and append that missing
# local-only refresh once the model assignments are in place.
CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL = 0x004A7695
CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL_BYTES = bytes.fromhex(
    "E9 76 F9 FE FF"
)
CLIENT_CHARACTER_DIRECTORY_NATIVE_REFRESH = 0x00497010
CLIENT_CHARACTER_DIRECTORY_LOCAL_UI_REFRESH = 0x004934F0
CLIENT_UI_CHILD_BY_INDEX = 0x0041B410
CLIENT_UI_ROOT = 0x0075F6E8
CLIENT_CHARACTER_SELECTED_INDEX = 0x0075F750
# The native Confirm path's only C_C9 packet prefix.  Arm source A here, while
# page 7 is still current, instead of after S_CB.  Arming after S_CB makes any
# retained LOGIN socket's next S_SERVER_LIST auto-send `C8 00` forever; arming
# immediately before C_C9 gives S_C9 its required /ltcharsvr role marker with
# no window in which a second server-list handler can consume it.
CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX = 0x00492ECC
CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES = bytes.fromhex("68 C9 00 00 00")
# A retained LOGIN transport can deliver S_SERVER_LIST after the GAME transport
# has already built character page 7.  The original official-launcher shortcut
# in sub_48FAA0 then emits C35 + an empty cached-source C8 and reconnects every
# ~11 seconds.  Ignore only that stale packet while page 7 is current; the first
# S_SERVER_LIST still enters the unmodified native source selector.
CLIENT_SERVER_LIST_HANDLER = 0x0048FAA0
CLIENT_SERVER_LIST_HANDLER_ENTRY = bytes.fromhex("55 8B EC 83 EC 44")
# sub_496EB0 has a second C_C8 producer before its network tail.  The retained
# old-launcher byte 64AF9D takes that path about ten seconds after page 7 is
# visible, sends the empty source pointer as `C8 00`, and replaces the stable
# selector with Loading.  Force both branches into the already-guarded return.
CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH = 0x00496F7B
CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH_BYTES = bytes.fromhex("74 37")
CLIENT_CHARACTER_PAGE_STABLE_RETURN_BRANCH_BYTES = bytes.fromhex("EB 37")
# sub_4966A0 turns the page-7 Server control into a passive image for the old
# official /ltcharsvr handoff by rewriting this shared UI descriptor.  The
# standalone client still needs that original control so users can choose a
# different configured character database without restarting the process.
CLIENT_CHARACTER_SERVER_CONTROL_TYPE = 0x00630EE8
CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_TYPE = 2
CLIENT_CHARACTER_SERVER_CONTROL_RESOURCE = 0x00630EEC
CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_RESOURCE = 0x18A8
CLIENT_CHARACTER_SERVER_CONTROL_CALLBACK = 0x00492FE0
# fn_ClientApplication_Run sets these role bytes from /ltcharsvr and the two
# event-server process names.  In sub_4A76D0 either non-zero byte deliberately
# bypasses the ordinary four-channel selector and emits C_CA(0) after a
# successful source-A character selection.  These bytes are part of the native
# launch contract and must remain unchanged.  Clearing them after page 7 mixes
# the /ltcharsvr teardown state with the ordinary selector path; that path then
# dereferences the already-released Tournament UI container at dword_64AD0C.
CLIENT_VS_SPECIAL_CHANNEL_MODE_A = 0x0064AF9E
CLIENT_VS_SPECIAL_CHANNEL_MODE_B = 0x0064AF9F
CLIENT_CHARACTER_SERVER_MODE = 0x0064AFA0
# S_C8's source-A result leaves this transition kind at 2. That value makes
# S_F0 reopen the source-selection transport and makes S_F5 suppress C_CF.
# A socket-delivered premium group-1 directory is instead the direct-room
# contract and must enter both handlers with the neutral value 0.
CLIENT_VS_TRANSITION_KIND = 0x0064AFB8
# The standalone launcher has no classic Lineage UI container at dword_64AD0C.
# Source-A character success normally calls sub_469CA0 to tear that container
# down before constructing the Tournament channel selector; with a null root
# its first sub_41B6E0 call writes to address 8.  Skip only that teardown call
# for the standalone native-login session.  The following sub_496A00 and
# sub_497020 calls remain byte-identical and build the original selector.
CLIENT_STANDALONE_SOURCE_CLEANUP_CALL = (
    0x004A776B,
    bytes.fromhex("E8 30 25 FC FF"),
    bytes.fromhex("90 90 90 90 90"),
)
# sub_48F500's S_E6 handler takes the immediate C_E2 branch only when this
# modal/deferred-cleanup flag is clear.  A stale one can survive the launcher
# handoff before its UI root exists in both selection modes.  That makes the
# deferred path dereference a null root or wait forever behind an empty modal.
CLIENT_LITO_NEWS_DEFER_FLAG = 0x0062C224
# The retained classic-source selector installs these three global dispatchers
# in sub_498F80.  Keeping them in the read-only runtime samples distinguishes
# a correctly rendered-but-deactivated modal from a bad physical coordinate.
CLIENT_GLOBAL_KEY_HANDLER = 0x0062C9B8
CLIENT_GLOBAL_MOUSE_HANDLER = 0x0062C9BC
CLIENT_GLOBAL_RENDER_HANDLER = 0x0062C9C0
# sub_496EB0 installs sub_495E40 as page 7's ordinary timed dispatcher.  The
# classic launcher handoff can leave sub_469B10/sub_469BD0 here instead; those
# two callbacks alternate LOGIN/GAME reconnects every ten seconds and rebuild
# an otherwise complete character page.  Restore the native page dispatcher
# only from the final S_CB completion hook, after page 7 and all rows exist.
CLIENT_CHARACTER_PAGE_TIMER_CALLBACK = 0x00495E40
CLIENT_LOGIN_RECONNECT_TIMER_CALLBACK = 0x00469B10
CLIENT_GAME_RECONNECT_TIMER_CALLBACK = 0x00469BD0
CLIENT_NATIVE_SOURCE_MODAL_ROOT = 0x0075FF7C
CLIENT_NATIVE_SOURCE_SELECTED_INDEX = 0x0075F6A0
CLIENT_NATIVE_SOURCE_COUNT = 0x0075F6E4
CLIENT_NATIVE_SOURCE_MODAL_MOUSE_HANDLER = 0x00498A90
CLIENT_NATIVE_SOURCE_MODAL_KEY_HANDLER = 0x00499A40
CLIENT_NATIVE_SOURCE_MODAL_RENDER_HANDLER = 0x00499A60
# LiTo initializes this byte to one before the VS UI exists.  The loopback
# handoff uses byte_64AF9D's native auto-enter branch, which returns before the
# ordinary in-game cleanup path and therefore does not dereference the old UI
# root.  Keep the flag enabled so S_C9 can activate the lobby controls after
# sub_46BA10 finishes constructing them.
CLIENT_GAME_UI_ACTIVE_FLAG = 0x0062C21C
CLIENT_LOBBY_HIT_TEST_CALLBACK = 0x00468FF0
CLIENT_LOBBY_HIT_TEST_CALLBACK_PROLOGUE = bytes.fromhex(
    "55 8B EC 8B 41 20"
)
# sub_468FF0 contains four optional-scene checks of the form
# `current == scene && scene->active`.  In the Tournament lobby all three
# pointers are legitimately null, so the equality succeeds and the original
# code dereferences address 0x44.  Patch only those null branches; returning
# from the whole callback prevents every bottom-toolbar button from receiving
# WM_LBUTTONDOWN/UP.
CLIENT_LOBBY_HIT_TEST_NULL_PATCHES = (
    (0x0046902D, bytes.fromhex("3B C8 75 07"), bytes.fromhex("85 C0 74 07")),
    (0x0046903D, bytes.fromhex("3B C8 75 1E"), bytes.fromhex("85 C0 74 1E")),
    (0x0046912F, bytes.fromhex("3B C8"), bytes.fromhex("85 C0")),
    (0x0046913B, bytes.fromhex("75 07"), bytes.fromhex("74 07")),
    (0x00469149, bytes.fromhex("3B C8 75 1E"), bytes.fromhex("85 C0 74 1E")),
)
CLIENT_LOBBY_OVERLAY_CALLBACK = 0x00469230
CLIENT_LOBBY_OVERLAY_CALLBACK_PROLOGUE = bytes.fromhex(
    "55 8B EC 81 EC 10 01 00 00"
)
# sub_46ACB0 is the legacy in-world LTshop callback.  Merely moving the
# window can synthesize a client-area mouse move under the stationary cursor,
# and the old dispatcher may invoke this callback while the Tournament lobby
# is active.  Its loc_46AC70 path assumes dword_647E14 already exists and
# otherwise crashes in sub_43A680 at 0x0043A687.
CLIENT_WORLD_SHOP_CALLBACK = 0x0046ACB0
CLIENT_WORLD_SHOP_CALLBACK_PROLOGUE = bytes.fromhex(
    "8B 0D F8 B3 64 00"
)
# S_LITO_READY_GAME's native UI transition calls launcher-owned controllers
# that do not exist in the standalone Tournament handoff.  A compact parser
# preserves the actual ready/cancel state without entering those stale calls.
CLIENT_TOURNAMENT_READY_HANDLER = 0x004A9550
CLIENT_TOURNAMENT_READY_HANDLER_PROLOGUE = bytes.fromhex(
    "55 8B EC 83 EC 20 53 56 8D 45 FF 57"
)
# A manual Tournament character-list click runs sub_4928C0 before READY.  The
# obsolete compact reconnect shortcut skipped that callback, leaving the world
# UI root null; sub_47CF00 then faulted at 0x0047D795.  The server now restores
# the counted native character list.  The compatibility bridge stages one
# native-shaped record, lets sub_4928C0 construct the missing root, and then
# re-evaluates the same root/controller choice made by sub_4A9550.  This last
# re-check is essential: calling sub_47CF00 a second time when dword_834C18 is
# already alive duplicates the native controller graph.
CLIENT_TOURNAMENT_WORLD_BOOTSTRAP = 0x004928C0
CLIENT_TOURNAMENT_WORLD_ROOT = 0x00648EB8
CLIENT_TOURNAMENT_READY_WORLD_CALL = 0x004A95D8
CLIENT_TOURNAMENT_READY_WORLD_CALL_BYTES = bytes.fromhex("E8 23 39 FD FF")
CLIENT_TOURNAMENT_READY_WORLD_CONSTRUCTOR = 0x0047CF00
CLIENT_TOURNAMENT_READY_WORLD_CONTROLLER = 0x00834C18
CLIENT_TOURNAMENT_READY_WORLD_REFRESH = 0x0047E360
# Numeric-only runtime markers bracket the complete S_D1 handler and the
# native S_EVENT subtype-33 C_ADDR producer.  They make packet-order failures
# distinguishable from renderer failures without reading client text buffers.
CLIENT_TOURNAMENT_READY_DISPATCH_CALL = 0x004B2F39
CLIENT_TOURNAMENT_READY_DISPATCH_CALL_BYTES = bytes.fromhex("E8 12 66 FF FF")
CLIENT_NATIVE_ADDR_CASE = 0x004B232E
CLIENT_NATIVE_ADDR_CASE_BYTES = bytes.fromhex("8B 1E 8B 7E 04")
CLIENT_NATIVE_ADDR_SEND_CALL = 0x004B2383
CLIENT_NATIVE_ADDR_SEND_CALL_BYTES = bytes.fromhex("E8 58 47 01 00")
CLIENT_NATIVE_EVENT_HANDLER = 0x004B07C0
CLIENT_NATIVE_PACKET_SEND = 0x004C6AE0
CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL = 0x004B2F99
CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL_BYTES = bytes.fromhex("E8 12 7C FF FF")
CLIENT_TOURNAMENT_COUNTDOWN_HANDLER = 0x004AABB0
# sub_4928C0 normally finds the highlighted classic character through a UI
# collection.  The Tournament reconnect socket has no such collection, so its
# compact profile is staged as record zero and this lookup block selects it.
CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION = 0x0049290D
CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES = bytes.fromhex(
    "BA0300000033C9E80718000083F8FF0F845F0600008D04808D14C08A0495CDFA62003CFF8D1C95A0FA62000F8430060000"
)
CLIENT_TOURNAMENT_BOOTSTRAP_RECORD = 0x0062FAA0
CLIENT_TOURNAMENT_BOOTSTRAP_RECORD_SIZE = 0xB4
CLIENT_LOCAL_OBJECT_ID = 0x0075AA48
# The late compatibility bootstrap must not send a second C_C9 after the
# server session is already in ROOM.  The original call's following add esp,12
# still balances the three arguments.
CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND = 0x00492F02
CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES = bytes.fromhex("E8D93B0300")
# Native S_CB subtype-0 fixed profile cells.  They are briefly populated before
# the channel UI clears them, so the runtime monitor snapshots them.
CLIENT_LITO_PROFILE_ID = 0x0064ABF4
CLIENT_LITO_PROFILE_CLAN = 0x0064AB70
CLIENT_LITO_PROFILE_CLAN_CAPACITY = 90
CLIENT_LITO_PROFILE_TYPE = 0x0064ABE6
CLIENT_LITO_PROFILE_MAX_HP = 0x0064ABEE
CLIENT_LITO_PROFILE_MAX_MP = 0x0064ABF0
CLIENT_LITO_PROFILE_ARMOR_CLASS = 0x0064ACCA
CLIENT_LITO_PROFILE_LEVEL_PLUS_12 = 0x0064ACC9
CLIENT_LITO_PROFILE_STRENGTH = 0x0064ACCC
CLIENT_LITO_PROFILE_CONSTITUTION = 0x0064ACD0
CLIENT_LITO_PROFILE_DEXTERITY = 0x0064ACD3
CLIENT_LITO_PROFILE_CHARISMA = 0x0064ACD1
CLIENT_LITO_PROFILE_INTELLIGENCE = 0x0064ACD2
CLIENT_LITO_PROFILE_WISDOM = 0x0064ACD4
CLIENT_LITO_PROFILE_SEX = 0x0064ACD5
# The room Start button is wired through this thunk.  A delayed bridge calls
# the native callback first (so C_D2 is emitted from the untouched producer),
# then completes the standalone character/world bootstrap on the UI thread,
# before the server's READY/countdown/world responses are consumed.
CLIENT_TOURNAMENT_READY_DIRECT_CALL = 0x0046B756
CLIENT_TOURNAMENT_READY_CONFIRM_CALL = 0x0046B7A2
CLIENT_TOURNAMENT_READY_CALL_BYTES = bytes.fromhex("E8 85 B3 05 00")
CLIENT_TOURNAMENT_READY_CONFIRM_CALL_BYTES = bytes.fromhex("E8 39 B3 05 00")
CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL = 0x00492E6B
CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL_BYTES = bytes.fromhex("6A 00 B9 B0 16")
CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY = 0x004928DE
CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY_BYTES = bytes.fromhex(
    "BA 7F 00 00 00 B9 01 00 00 00 E8 A3 5E 03 00"
)
CLIENT_TOURNAMENT_START_HANDLER = 0x004A98A0
CLIENT_TOURNAMENT_START_HANDLER_PROLOGUE = bytes.fromhex("A0 1C C2 62 00")
# sub_42EA50 opens files through one of the general Text/Sprite/Tile/Surf/Icon
# managers.  The 2008 Tournament client first checks one of sixteen SpriteXX
# Surf shards, then its 2007 base Sprite archive, and finally LiTo\LTSurf.  The
# supplied Sprite00-Sprite15 data was updated in 2014 and now contains names
# which collide with the older LTsurf archive (including 6257.img and
# 6258.img), so the newer shard wins and displays the wrong Tournament UI.
# The volatile hook below suppresses only a SpriteXX lookup whose name exists
# in LTsurf.  The caller then preserves the native 2007 base-Surf -> LTsurf
# fallback sequence; ordinary-only and original base-Surf entries are intact.
CLIENT_RESOURCE_OPEN = 0x0042EA50
CLIENT_RESOURCE_OPEN_PROLOGUE = bytes.fromhex("55 8B EC 83 EC 20")
CLIENT_BASE_SURF_MANAGER_POINTER = 0x0064798C
CLIENT_LT_SURF_MANAGER_POINTER = 0x006479A0
CLIENT_SHARD_SURF_MANAGER_ARRAY = 0x006479E8
CLIENT_SHARD_SURF_MANAGER_COUNT = 16
CLIENT_FREE = 0x004F103C
# Native sub_4A98A0 asks sub_469CA0 to switch to phase 4, then installs the
# Tournament input/timer callbacks.  These are the exact global dependencies
# touched by that transition.  Keep them in the normal runtime telemetry so a
# failed arena handoff can be diagnosed without guessing from a crash dump.
CLIENT_ARENA_START_GLOBALS = {
    "ui_group_primary": 0x0064AD0C,
    "ui_controller_a": 0x0064A990,
    "ui_controller_b": 0x0064A98C,
    "ui_controller_c": 0x0064A904,
    "ui_group_secondary_a": 0x0064AD10,
    "ui_group_secondary_b": 0x0064AD14,
    "ui_group_secondary_c": 0x0064AD08,
    "ui_controller_d": 0x0064B09C,
    "ui_controller_e": 0x0064B098,
    "ui_control_array": 0x0064B908,
    "tournament_table": 0x0064AFE0,
    "tournament_index": 0x0075A898,
    "main_window": 0x0064BBEC,
    "arena_width": 0x0064B900,
    "arena_height": 0x0064B904,
}
# The 640x480 character page is laid out relative to these two native client
# cells.  Entering a rental world changes them to the arena viewport (333x217)
# and a reconnect does not restore them, pushing page 7's confirm/exit row
# below the window.  Fresh LOGIN -> GAME construction consistently owns the
# original 60x152 pair for this verified LiTo.bin build.
CLIENT_CHARACTER_PAGE_ARENA_WIDTH = 60
CLIENT_CHARACTER_PAGE_ARENA_HEIGHT = 152
CLIENT_ENDPOINT_UI_SCENE_POINTERS = tuple(
    CLIENT_ARENA_START_GLOBALS[name]
    for name in (
        "ui_group_primary",
        "ui_controller_a",
        "ui_controller_b",
        "ui_controller_c",
        "ui_group_secondary_a",
        "ui_group_secondary_b",
        "ui_group_secondary_c",
        "ui_controller_d",
        "ui_controller_e",
        "ui_control_array",
    )
)
CLIENT_WORLD_SCENE = 0x00648EBC
CLIENT_STATIC_SCENE = 0x00834888
CLIENT_PHASE = 0x0064AF98
# Native inventory lookup and ordinary use-type-0 producer.  The isolated
# transfer-stone fixture owns this one session-local object id; no arbitrary
# object id or producer address is accepted from the command line.
CLIENT_INVENTORY_LOOKUP = 0x00444900
CLIENT_INVENTORY_LOOKUP_SIGNATURE = bytes.fromhex("55 8B EC 56 8B 71 2C")
CLIENT_DEFAULT_ITEM_USE = 0x00445BD0
CLIENT_DEFAULT_ITEM_USE_SIGNATURE = bytes.fromhex("56 8B F1 E8 68 5D FC FF")
TRANSFER_STONE_PROBE_OBJECT_ID = 0x6C000001
CLIENT_VERSION_LOGIN_BRANCH = 0x004A3679
CLIENT_VERSION_LOGIN_BRANCH_SIGNATURE = bytes.fromhex("A0 55 F7 75 00")
# After a VS redirect, the original launcher asks LiTo to run a legacy token
# generator immediately after its ordinary C_LOGIN.  That generator depends
# on official-launcher state which a standalone local launcher does not have.
CLIENT_LEGACY_VS_TOKEN_CALL = (
    0x004A36DA,
    bytes.fromhex("E8 B1 B8 FC FF"),
    bytes.fromhex("90 90 90 90 90"),
)
KNOWN_PROTECTED_SHA256 = "CE6EE68ADA8FD37E6C096B1A27E46CB6888F1179F85DEFB5007AC60287360292"
KNOWN_LAUNCHER_SHA256 = "22FF270BD181EC97206678BF7CFF5E6648F6EF41F704DC1AA763473BDBD146C3"
DISPLAY_MODE_PATCHES = {
    # sub_4233C0 writes byte_6476C8 after checking desktop color depth.
    # 0 selects DDSCL_NORMAL (windowed); 1 selects exclusive 640x480x16.
    "windowed": (
        0x004233D3,
        bytes.fromhex("C6 05 C8 76 64 00 01"),
        bytes.fromhex("C6 05 C8 76 64 00 00"),
    ),
    "fullscreen": (
        0x004233D1,
        bytes.fromhex("74 07"),
        bytes.fromhex("90 90"),
    ),
}


class STARTUPINFOW(ctypes.Structure):
    _fields_ = [
        ("cb", wt.DWORD),
        ("lpReserved", wt.LPWSTR),
        ("lpDesktop", wt.LPWSTR),
        ("lpTitle", wt.LPWSTR),
        ("dwX", wt.DWORD),
        ("dwY", wt.DWORD),
        ("dwXSize", wt.DWORD),
        ("dwYSize", wt.DWORD),
        ("dwXCountChars", wt.DWORD),
        ("dwYCountChars", wt.DWORD),
        ("dwFillAttribute", wt.DWORD),
        ("dwFlags", wt.DWORD),
        ("wShowWindow", wt.WORD),
        ("cbReserved2", wt.WORD),
        ("lpReserved2", ctypes.POINTER(ctypes.c_ubyte)),
        ("hStdInput", wt.HANDLE),
        ("hStdOutput", wt.HANDLE),
        ("hStdError", wt.HANDLE),
    ]


class PROCESS_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("hProcess", wt.HANDLE),
        ("hThread", wt.HANDLE),
        ("dwProcessId", wt.DWORD),
        ("dwThreadId", wt.DWORD),
    ]


class EXCEPTION_RECORD64(ctypes.Structure):
    _fields_ = [
        ("ExceptionCode", wt.DWORD),
        ("ExceptionFlags", wt.DWORD),
        ("ExceptionRecord", ctypes.c_void_p),
        ("ExceptionAddress", ctypes.c_void_p),
        ("NumberParameters", wt.DWORD),
        ("ExceptionInformation", ctypes.c_ulonglong * 15),
    ]


class EXCEPTION_DEBUG_INFO(ctypes.Structure):
    _fields_ = [
        ("ExceptionRecord", EXCEPTION_RECORD64),
        ("dwFirstChance", wt.DWORD),
    ]


class DEBUG_EVENT_UNION(ctypes.Union):
    _fields_ = [
        ("Exception", EXCEPTION_DEBUG_INFO),
        ("raw", ctypes.c_ubyte * 160),
    ]


class DEBUG_EVENT(ctypes.Structure):
    _fields_ = [
        ("dwDebugEventCode", wt.DWORD),
        ("dwProcessId", wt.DWORD),
        ("dwThreadId", wt.DWORD),
        ("u", DEBUG_EVENT_UNION),
    ]


class PROCESSENTRY32W(ctypes.Structure):
    _fields_ = [
        ("dwSize", wt.DWORD),
        ("cntUsage", wt.DWORD),
        ("th32ProcessID", wt.DWORD),
        ("th32DefaultHeapID", ctypes.c_size_t),
        ("th32ModuleID", wt.DWORD),
        ("cntThreads", wt.DWORD),
        ("th32ParentProcessID", wt.DWORD),
        ("pcPriClassBase", wt.LONG),
        ("dwFlags", wt.DWORD),
        ("szExeFile", wt.WCHAR * 260),
    ]


class MODULEENTRY32W(ctypes.Structure):
    _fields_ = [
        ("dwSize", wt.DWORD),
        ("th32ModuleID", wt.DWORD),
        ("th32ProcessID", wt.DWORD),
        ("GlblcntUsage", wt.DWORD),
        ("ProccntUsage", wt.DWORD),
        ("modBaseAddr", ctypes.POINTER(ctypes.c_ubyte)),
        ("modBaseSize", wt.DWORD),
        ("hModule", wt.HMODULE),
        ("szModule", wt.WCHAR * 256),
        ("szExePath", wt.WCHAR * 260),
    ]


class THREADENTRY32(ctypes.Structure):
    _fields_ = [
        ("dwSize", wt.DWORD),
        ("cntUsage", wt.DWORD),
        ("th32ThreadID", wt.DWORD),
        ("th32OwnerProcessID", wt.DWORD),
        ("tpBasePri", wt.LONG),
        ("tpDeltaPri", wt.LONG),
        ("dwFlags", wt.DWORD),
    ]


class WOW64_FLOATING_SAVE_AREA(ctypes.Structure):
    _fields_ = [
        ("ControlWord", wt.DWORD),
        ("StatusWord", wt.DWORD),
        ("TagWord", wt.DWORD),
        ("ErrorOffset", wt.DWORD),
        ("ErrorSelector", wt.DWORD),
        ("DataOffset", wt.DWORD),
        ("DataSelector", wt.DWORD),
        ("RegisterArea", ctypes.c_ubyte * 80),
        ("Cr0NpxState", wt.DWORD),
    ]


class WOW64_CONTEXT(ctypes.Structure):
    _fields_ = [
        ("ContextFlags", wt.DWORD),
        ("Dr0", wt.DWORD),
        ("Dr1", wt.DWORD),
        ("Dr2", wt.DWORD),
        ("Dr3", wt.DWORD),
        ("Dr6", wt.DWORD),
        ("Dr7", wt.DWORD),
        ("FloatSave", WOW64_FLOATING_SAVE_AREA),
        ("SegGs", wt.DWORD),
        ("SegFs", wt.DWORD),
        ("SegEs", wt.DWORD),
        ("SegDs", wt.DWORD),
        ("Edi", wt.DWORD),
        ("Esi", wt.DWORD),
        ("Ebx", wt.DWORD),
        ("Edx", wt.DWORD),
        ("Ecx", wt.DWORD),
        ("Eax", wt.DWORD),
        ("Ebp", wt.DWORD),
        ("Eip", wt.DWORD),
        ("SegCs", wt.DWORD),
        ("EFlags", wt.DWORD),
        ("Esp", wt.DWORD),
        ("SegSs", wt.DWORD),
        ("ExtendedRegisters", ctypes.c_ubyte * 512),
    ]


class IO_COUNTERS(ctypes.Structure):
    _fields_ = [
        ("ReadOperationCount", ctypes.c_ulonglong),
        ("WriteOperationCount", ctypes.c_ulonglong),
        ("OtherOperationCount", ctypes.c_ulonglong),
        ("ReadTransferCount", ctypes.c_ulonglong),
        ("WriteTransferCount", ctypes.c_ulonglong),
        ("OtherTransferCount", ctypes.c_ulonglong),
    ]


class FILETIME(ctypes.Structure):
    _fields_ = [("dwLowDateTime", wt.DWORD), ("dwHighDateTime", wt.DWORD)]


class PROCESS_MEMORY_COUNTERS_EX(ctypes.Structure):
    _fields_ = [
        ("cb", wt.DWORD),
        ("PageFaultCount", wt.DWORD),
        ("PeakWorkingSetSize", ctypes.c_size_t),
        ("WorkingSetSize", ctypes.c_size_t),
        ("QuotaPeakPagedPoolUsage", ctypes.c_size_t),
        ("QuotaPagedPoolUsage", ctypes.c_size_t),
        ("QuotaPeakNonPagedPoolUsage", ctypes.c_size_t),
        ("QuotaNonPagedPoolUsage", ctypes.c_size_t),
        ("PagefileUsage", ctypes.c_size_t),
        ("PeakPagefileUsage", ctypes.c_size_t),
        ("PrivateUsage", ctypes.c_size_t),
    ]


class JOBOBJECT_BASIC_LIMIT_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("PerProcessUserTimeLimit", ctypes.c_longlong),
        ("PerJobUserTimeLimit", ctypes.c_longlong),
        ("LimitFlags", wt.DWORD),
        ("MinimumWorkingSetSize", ctypes.c_size_t),
        ("MaximumWorkingSetSize", ctypes.c_size_t),
        ("ActiveProcessLimit", wt.DWORD),
        ("Affinity", ctypes.c_size_t),
        ("PriorityClass", wt.DWORD),
        ("SchedulingClass", wt.DWORD),
    ]


class JOBOBJECT_EXTENDED_LIMIT_INFORMATION(ctypes.Structure):
    _fields_ = [
        ("BasicLimitInformation", JOBOBJECT_BASIC_LIMIT_INFORMATION),
        ("IoInfo", IO_COUNTERS),
        ("ProcessMemoryLimit", ctypes.c_size_t),
        ("JobMemoryLimit", ctypes.c_size_t),
        ("PeakProcessMemoryUsed", ctypes.c_size_t),
        ("PeakJobMemoryUsed", ctypes.c_size_t),
    ]


kernel32 = ctypes.WinDLL("kernel32", use_last_error=True)
kernel32.CreateProcessW.restype = wt.BOOL
kernel32.CreateToolhelp32Snapshot.restype = wt.HANDLE
kernel32.Process32FirstW.restype = wt.BOOL
kernel32.Process32NextW.restype = wt.BOOL
kernel32.Module32FirstW.restype = wt.BOOL
kernel32.Module32NextW.restype = wt.BOOL
kernel32.OpenProcess.restype = wt.HANDLE
kernel32.OpenThread.restype = wt.HANDLE
kernel32.QueryFullProcessImageNameW.restype = wt.BOOL
kernel32.GetExitCodeProcess.restype = wt.BOOL
kernel32.CreateJobObjectW.restype = wt.HANDLE
kernel32.SetInformationJobObject.restype = wt.BOOL
kernel32.AssignProcessToJobObject.restype = wt.BOOL
kernel32.ResumeThread.restype = wt.DWORD
kernel32.GetProcessTimes.restype = wt.BOOL
kernel32.GetProcessIoCounters.restype = wt.BOOL
kernel32.GetProcessHandleCount.restype = wt.BOOL
kernel32.SetProcessAffinityMask.restype = wt.BOOL
kernel32.SuspendThread.restype = wt.DWORD
kernel32.ResumeThread.restype = wt.DWORD
kernel32.Wow64GetThreadContext.restype = wt.BOOL
kernel32.Wow64SetThreadContext.restype = wt.BOOL
kernel32.ReadProcessMemory.restype = wt.BOOL
kernel32.WriteProcessMemory.restype = wt.BOOL
kernel32.VirtualAllocEx.restype = ctypes.c_void_p
kernel32.VirtualFreeEx.restype = wt.BOOL
kernel32.VirtualProtectEx.restype = wt.BOOL
kernel32.FlushInstructionCache.restype = wt.BOOL
kernel32.QueueUserAPC.restype = wt.DWORD
kernel32.CreateRemoteThread.restype = wt.HANDLE
kernel32.WaitForSingleObject.restype = wt.DWORD
kernel32.GetExitCodeThread.restype = wt.BOOL
kernel32.WaitForDebugEvent.restype = wt.BOOL
kernel32.ContinueDebugEvent.restype = wt.BOOL
user32 = ctypes.WinDLL("user32", use_last_error=True)
EnumWindowsProc = ctypes.WINFUNCTYPE(ctypes.c_bool, wt.HWND, wt.LPARAM)
user32.EnumWindows.restype = wt.BOOL
user32.IsWindowVisible.restype = wt.BOOL
user32.GetWindowThreadProcessId.restype = wt.DWORD
user32.GetWindowTextLengthW.restype = ctypes.c_int
user32.GetWindowTextW.restype = ctypes.c_int
user32.GetClassNameW.restype = ctypes.c_int
user32.PostMessageW.restype = wt.BOOL
psapi = ctypes.WinDLL("psapi", use_last_error=True)
psapi.GetProcessMemoryInfo.restype = wt.BOOL


def win_error(label: str) -> OSError:
    return OSError(ctypes.get_last_error(), f"{label}: {ctypes.WinError(ctypes.get_last_error())}")


def process_snapshot() -> dict[int, dict[str, Any]]:
    snapshot = kernel32.CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0)
    if snapshot == INVALID_HANDLE_VALUE:
        return {}
    rows: dict[int, dict[str, Any]] = {}
    try:
        entry = PROCESSENTRY32W()
        entry.dwSize = ctypes.sizeof(entry)
        ok = kernel32.Process32FirstW(snapshot, ctypes.byref(entry))
        while ok:
            pid = int(entry.th32ProcessID)
            rows[pid] = {
                "pid": pid,
                "parent_pid": int(entry.th32ParentProcessID),
                "name": entry.szExeFile,
                "threads": int(entry.cntThreads),
            }
            ok = kernel32.Process32NextW(snapshot, ctypes.byref(entry))
    finally:
        kernel32.CloseHandle(snapshot)
    return rows


def descendants(root_pid: int, rows: dict[int, dict[str, Any]]) -> set[int]:
    found = {root_pid}
    changed = True
    while changed:
        changed = False
        for pid, row in rows.items():
            if pid not in found and row["parent_pid"] in found:
                found.add(pid)
                changed = True
    return found


def process_path(pid: int) -> str:
    handle = kernel32.OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, False, pid)
    if not handle:
        return ""
    try:
        size = wt.DWORD(32768)
        buffer = ctypes.create_unicode_buffer(size.value)
        if kernel32.QueryFullProcessImageNameW(handle, 0, buffer, ctypes.byref(size)):
            return buffer.value
        return ""
    finally:
        kernel32.CloseHandle(handle)


def module_snapshot(pid: int) -> list[dict[str, Any]]:
    snapshot = kernel32.CreateToolhelp32Snapshot(
        TH32CS_SNAPMODULE | TH32CS_SNAPMODULE32, pid
    )
    if snapshot == INVALID_HANDLE_VALUE:
        return []
    rows: list[dict[str, Any]] = []
    try:
        entry = MODULEENTRY32W()
        entry.dwSize = ctypes.sizeof(entry)
        ok = kernel32.Module32FirstW(snapshot, ctypes.byref(entry))
        while ok:
            rows.append(
                {
                    "name": entry.szModule,
                    "path": entry.szExePath,
                    "base": f"0x{ctypes.cast(entry.modBaseAddr, ctypes.c_void_p).value or 0:08X}",
                    "size": int(entry.modBaseSize),
                }
            )
            ok = kernel32.Module32NextW(snapshot, ctypes.byref(entry))
    finally:
        kernel32.CloseHandle(snapshot)
    return rows


def remote_export_address(pid: int, module_name: str, export_name: str) -> int:
    module = next(
        (
            row
            for row in module_snapshot(pid)
            if row["name"].casefold() == module_name.casefold()
            and int(row["base"], 16) <= 0xFFFFFFFF
        ),
        None,
    )
    if module is None:
        raise RuntimeError(f"{module_name} is not loaded in the WOW64 process")
    pe = pefile.PE(module["path"], fast_load=True)
    pe.parse_data_directories(
        directories=[pefile.DIRECTORY_ENTRY["IMAGE_DIRECTORY_ENTRY_EXPORT"]]
    )
    symbol = next(
        (
            item
            for item in pe.DIRECTORY_ENTRY_EXPORT.symbols
            if item.name is not None
            and item.name.decode("ascii", errors="strict") == export_name
        ),
        None,
    )
    if symbol is None:
        raise RuntimeError(f"{module_name}!{export_name} export was not found")
    return int(module["base"], 16) + int(symbol.address)


def install_process_exit_trace(process_handle: int, pid: int) -> dict[str, Any] | None:
    """Pause at the WOW64 RtlExitUserProcess entry and preserve its caller.

    This diagnostic hook never ships in the normal launch path.  It records the
    original stack pointer, return address and status, then spins so the probe can
    read the still-live stack before terminating the job at its normal timeout.
    """
    try:
        target = remote_export_address(pid, "ntdll.dll", "RtlExitUserProcess")
    except (RuntimeError, OSError):
        return None
    original = read_process(process_handle, target, 5)
    if len(original) != 5:
        return None
    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate an x86 process-exit trace page")

    telemetry = remote + 0x100
    caller_address = telemetry + 4
    status_address = telemetry + 8
    stack_pointer_address = telemetry + 12
    stub = bytearray()
    stub += b"\x8B\x04\x24\xA3" + struct.pack("<I", caller_address)
    stub += b"\x8B\x44\x24\x04\xA3" + struct.pack("<I", status_address)
    stub += b"\x8B\xC4\xA3" + struct.pack("<I", stack_pointer_address)
    stub += b"\xF0\xFF\x05" + struct.pack("<I", telemetry)
    stub += b"\xEB\xFE"
    data = bytes(stub) + b"\x00" * (0x110 - len(stub))
    if write_process(process_handle, remote, data) != len(data):
        raise RuntimeError("short write while installing process-exit trace")
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(remote), len(data)
    ):
        raise win_error("FlushInstructionCache(process-exit trace)")
    patch = b"\xE9" + struct.pack("<i", remote - (target + 5))
    write_process_protected(process_handle, target, patch)
    return {
        "status": "installed",
        "target": f"0x{target:08X}",
        "target_original": original.hex(" ").upper(),
        "target_patch": patch.hex(" ").upper(),
        "hook_address": f"0x{remote:08X}",
        "telemetry_address": f"0x{telemetry:08X}",
        "diagnostic_only": True,
        "executable_modified": False,
    }


def install_unhandled_exception_trace(
    process_handle: int, pid: int
) -> dict[str, Any] | None:
    """Park only after Windows has classified an exception as unhandled."""
    try:
        target = remote_export_address(pid, "ntdll.dll", "RtlUnhandledExceptionFilter")
    except (RuntimeError, OSError):
        return None
    original = read_process(process_handle, target, 5)
    if original != bytes.fromhex("8B FF 55 8B EC"):
        return None
    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate an x86 unhandled-exception trace page")

    telemetry = remote + 0x100
    stub = bytearray()
    stub += b"\x8B\x44\x24\x04"  # eax = EXCEPTION_POINTERS*
    stub += b"\xA3" + struct.pack("<I", telemetry + 4)
    stub += b"\x8B\x08"  # ecx = EXCEPTION_RECORD*
    stub += b"\x8B\x50\x04"  # edx = WOW64_CONTEXT*
    for source, destination in (
        (b"\x8B\x01", 8),  # code
        (b"\x8B\x41\x0C", 12),  # exception address
        (b"\x8B\x41\x14", 16),  # access type
        (b"\x8B\x41\x18", 20),  # fault address
    ):
        stub += source + b"\xA3" + struct.pack("<I", telemetry + destination)
    for destination, context_offset in (
        (24, 0xB8),  # EIP
        (28, 0xB0),  # EAX
        (32, 0xA4),  # EBX
        (36, 0xAC),  # ECX
        (40, 0xA8),  # EDX
        (44, 0xA0),  # ESI
        (48, 0x9C),  # EDI
        (52, 0xB4),  # EBP
        (56, 0xC4),  # ESP
    ):
        stub += b"\x8B\x82" + struct.pack("<I", context_offset)
        stub += b"\xA3" + struct.pack("<I", telemetry + destination)
    stub += b"\xF0\xFF\x05" + struct.pack("<I", telemetry)
    stub += b"\xEB\xFE"
    data = bytes(stub) + b"\x00" * (0x140 - len(stub))
    if write_process(process_handle, remote, data) != len(data):
        raise RuntimeError("short write while installing unhandled-exception trace")
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(remote), len(data)
    ):
        raise win_error("FlushInstructionCache(unhandled-exception trace)")
    patch = b"\xE9" + struct.pack("<i", remote - (target + 5))
    write_process_protected(process_handle, target, patch)
    return {
        "status": "installed",
        "target": f"0x{target:08X}",
        "target_original": original.hex(" ").upper(),
        "target_patch": patch.hex(" ").upper(),
        "hook_address": f"0x{remote:08X}",
        "telemetry_address": f"0x{telemetry:08X}",
        "diagnostic_only": True,
        "executable_modified": False,
    }


def update_unhandled_exception_trace(
    process_handle: int, trace: dict[str, Any]
) -> None:
    telemetry = int(trace["telemetry_address"], 16)
    data = read_process(process_handle, telemetry, 60)
    if len(data) != 60:
        return
    values = struct.unpack("<15I", data)
    if not values[0]:
        return
    (
        count,
        exception_pointers,
        code,
        exception_address,
        access_type,
        fault_address,
        eip,
        eax,
        ebx,
        ecx,
        edx,
        esi,
        edi,
        ebp,
        esp,
    ) = values
    trace["status"] = "unhandled_exception_intercepted"
    trace["telemetry"] = {
        "count": count,
        "exception_pointers": f"0x{exception_pointers:08X}",
        "code": f"0x{code:08X}",
        "exception_address": f"0x{exception_address:08X}",
        "access_type": {0: "read", 1: "write", 8: "execute"}.get(
            access_type, f"unknown_{access_type}"
        ),
        "fault_address": f"0x{fault_address:08X}",
        "registers": {
            name: f"0x{value:08X}"
            for name, value in (
                ("eip", eip),
                ("eax", eax),
                ("ebx", ebx),
                ("ecx", ecx),
                ("edx", edx),
                ("esi", esi),
                ("edi", edi),
                ("ebp", ebp),
                ("esp", esp),
            )
        },
        "instruction_window": {
            "base": f"0x{max(0, eip - 0x20):08X}",
            "hex": read_process(process_handle, max(0, eip - 0x20), 0x80)
            .hex()
            .upper(),
        },
        "esp_stack_window": {
            "base": f"0x{esp:08X}",
            "hex": read_process(process_handle, esp, 0x400).hex().upper(),
        },
        "ebp_stack_window": {
            "base": f"0x{max(0, ebp - 0x80):08X}",
            "hex": read_process(process_handle, max(0, ebp - 0x80), 0x300)
            .hex()
            .upper(),
        },
    }


def update_process_exit_trace(
    process_handle: int, trace: dict[str, Any]
) -> None:
    raw = read_process(process_handle, int(trace["telemetry_address"], 16), 16)
    if len(raw) != 16:
        return
    call_count, caller, exit_status, stack_pointer = struct.unpack("<4I", raw)
    trace["telemetry"] = {
        "call_count": call_count,
        "caller": f"0x{caller:08X}",
        "exit_status": f"0x{exit_status:08X}",
        "stack_pointer": f"0x{stack_pointer:08X}",
    }
    if not call_count or not stack_pointer or "stack_window" in trace:
        return
    stack = read_process(process_handle, stack_pointer, 0x100)
    stack_values = list(
        struct.unpack(
            f"<{len(stack) // 4}I", stack[: len(stack) // 4 * 4]
        )
    )
    trace["status"] = "exit_intercepted"
    message = read_process(process_handle, 0x0064B6B0, 28)
    quit_branch = read_process(process_handle, 0x004820DB, 2)
    trace["client_exit_state"] = {
        "main_quit_branch": quit_branch.hex(" ").upper(),
        "message_hex": message.hex(" ").upper(),
    }
    if len(message) == 28:
        hwnd, message_id, wparam, lparam, message_time, point_x, point_y = (
            struct.unpack("<7I", message)
        )
        trace["client_exit_state"].update(
            {
                "hwnd": f"0x{hwnd:08X}",
                "message": f"0x{message_id:08X}",
                "wparam": f"0x{wparam:08X}",
                "lparam": f"0x{lparam:08X}",
                "time": message_time,
                "point": [point_x, point_y],
            }
        )
    trace["stack_window"] = {
        "base": f"0x{stack_pointer:08X}",
        "size": len(stack),
        "hex": stack.hex().upper(),
        "dwords": [
            f"0x{value:08X}"
            for value in stack_values
        ],
    }
    client_code_windows: list[dict[str, Any]] = []
    for value in dict.fromkeys(stack_values):
        if not 0x00400020 <= value < 0x00AC4000:
            continue
        base = value - 0x20
        code = read_process(process_handle, base, 0x60)
        client_code_windows.append(
            {
                "stack_value": f"0x{value:08X}",
                "base": f"0x{base:08X}",
                "size": len(code),
                "hex": code.hex(" ").upper(),
            }
        )
    trace["client_code_windows"] = client_code_windows


def thread_ids(pid: int) -> list[int]:
    snapshot = kernel32.CreateToolhelp32Snapshot(TH32CS_SNAPTHREAD, 0)
    if snapshot == INVALID_HANDLE_VALUE:
        return []
    values: list[int] = []
    try:
        entry = THREADENTRY32()
        entry.dwSize = ctypes.sizeof(entry)
        ok = kernel32.Thread32First(snapshot, ctypes.byref(entry))
        while ok:
            if int(entry.th32OwnerProcessID) == pid:
                values.append(int(entry.th32ThreadID))
            ok = kernel32.Thread32Next(snapshot, ctypes.byref(entry))
    finally:
        kernel32.CloseHandle(snapshot)
    return values


def read_process(handle: int, address: int, size: int) -> bytes:
    buffer = (ctypes.c_ubyte * size)()
    read = ctypes.c_size_t()
    kernel32.ReadProcessMemory(
        handle, ctypes.c_void_p(address), buffer, size, ctypes.byref(read)
    )
    return bytes(buffer[: read.value])


def post_native_character_row_selection(pid: int, selected_index: int) -> dict[str, Any]:
    """Activate one completed page-7 row through LiTo's ordinary input path."""

    matches: list[int] = []

    @EnumWindowsProc
    def visit(hwnd: int, _parameter: int) -> bool:
        if not user32.IsWindowVisible(hwnd):
            return True
        window_pid = wt.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(window_pid))
        if int(window_pid.value) != pid:
            return True
        length = user32.GetWindowTextLengthW(hwnd)
        if length <= 0:
            return True
        title = ctypes.create_unicode_buffer(length + 1)
        user32.GetWindowTextW(hwnd, title, len(title))
        class_name = ctypes.create_unicode_buffer(128)
        user32.GetClassNameW(hwnd, class_name, len(class_name))
        if (
            title.value == "Lineage Windows Client"
            and class_name.value != "#32770"
        ):
            matches.append(int(hwnd))
        return True

    if not user32.EnumWindows(visit, 0):
        return {"status": "window_enumeration_failed", "posted": False}
    if len(matches) != 1:
        return {
            "status": "waiting_for_unique_client_window",
            "posted": False,
            "matching_window_count": len(matches),
        }
    index = max(0, min(3, int(selected_index)))
    logical_x = 30 + index * 80
    logical_y = 76
    packed = ((logical_y & 0xFFFF) << 16) | (logical_x & 0xFFFF)
    hwnd = matches[0]
    moved = bool(user32.PostMessageW(hwnd, WM_MOUSEMOVE, 0, packed))
    pressed = bool(
        user32.PostMessageW(hwnd, WM_LBUTTONDOWN, MK_LBUTTON, packed)
    )
    time.sleep(0.08)
    released = bool(user32.PostMessageW(hwnd, WM_LBUTTONUP, 0, packed))
    posted = moved and pressed and released
    return {
        "status": "native_row_activated" if posted else "post_message_failed",
        "posted": posted,
        "hwnd": f"0x{hwnd:08X}",
        "selected_index": index,
        "logical_coordinate": [logical_x, logical_y],
        "network_packet_emitted": False,
    }


def write_process(handle: int, address: int, data: bytes) -> int:
    buffer = (ctypes.c_ubyte * len(data)).from_buffer_copy(data)
    written = ctypes.c_size_t()
    if not kernel32.WriteProcessMemory(
        handle, ctypes.c_void_p(address), buffer, len(data), ctypes.byref(written)
    ):
        raise win_error(f"WriteProcessMemory(0x{address:08X})")
    return int(written.value)


def write_process_protected(handle: int, address: int, data: bytes) -> int:
    old_protect = wt.DWORD()
    if not kernel32.VirtualProtectEx(
        handle,
        ctypes.c_void_p(address),
        len(data),
        PAGE_EXECUTE_READWRITE,
        ctypes.byref(old_protect),
    ):
        raise win_error(f"VirtualProtectEx(0x{address:08X})")
    try:
        written = write_process(handle, address, data)
        if not kernel32.FlushInstructionCache(handle, ctypes.c_void_p(address), len(data)):
            raise win_error(f"FlushInstructionCache(0x{address:08X})")
        return written
    finally:
        restored = wt.DWORD()
        kernel32.VirtualProtectEx(
            handle,
            ctypes.c_void_p(address),
            len(data),
            old_protect.value,
            ctypes.byref(restored),
        )


def install_lito_surface_precedence(
    process_handle: int,
) -> dict[str, Any] | None:
    """Ignore only newer SpriteXX names which shadow LiTo's LTsurf archive.

    For a Surf shard request the hook probes LTsurf through the same native
    routine.  A hit makes the shard lookup report not-found, so the caller
    continues with its original 2007 base-Surf lookup and native LT fallback.
    The exact 400c.img..405c.img shard aliases are also reported not-found:
    those later tooltip images otherwise shadow the original 19x21
    400.img..405.img Tournament toolbar.  Other managers and names are never
    changed.
    """

    current = read_process(
        process_handle,
        CLIENT_RESOURCE_OPEN,
        len(CLIENT_RESOURCE_OPEN_PROLOGUE),
    )
    if current != CLIENT_RESOURCE_OPEN_PROLOGUE:
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate an x86 LTsurf precedence page")

    hook_address = remote
    trampoline_address = remote + 0x1C0
    telemetry_address = remote + 0x240
    attempts_address = telemetry_address
    hits_address = telemetry_address + 4
    misses_address = telemetry_address + 8
    image_6257_address = telemetry_address + 12
    image_6258_address = telemetry_address + 16
    image_7400_7404_address = telemetry_address + 20
    image_400c_405c_address = telemetry_address + 24

    trampoline = bytearray(CLIENT_RESOURCE_OPEN_PROLOGUE)
    trampoline += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_RESOURCE_OPEN + len(CLIENT_RESOURCE_OPEN_PROLOGUE))
        - (trampoline_address + len(trampoline) + 5),
    )

    hook = bytearray()
    fixups: list[tuple[int, str]] = []
    labels: dict[str, int] = {}

    def emit_short_jump(opcode: int, label: str) -> None:
        hook.append(opcode)
        fixups.append((len(hook), label))
        hook.append(0)

    # The mixed base-Surf index aliases LiTo 7400..7404 to ordinary Lineage
    # toolbar art.  LTsurf contains exact records for all five Tournament
    # icons, so bypass only those verified base-manager aliases.  Literal
    # 400.img..405.img requests must stay untouched: the native bottom toolbar
    # deliberately uses those pairs, and 404/405 is its real exit button.
    hook += b"\xA1" + struct.pack("<I", CLIENT_BASE_SURF_MANAGER_POINTER)
    hook += b"\x3B\xC8"  # cmp ecx,eax
    emit_short_jump(0x75, "manager_scan")
    hook += b"\x8B\x54\x24\x04\x85\xD2"  # filename; null guard
    emit_short_jump(0x74, "manager_scan")
    hook += b"\x8B\x02\x25\xFF\xFF\xFF\x00"  # first three characters
    hook += b"\x3D" + struct.pack("<I", 0x00303437)  # "740"
    emit_short_jump(0x75, "manager_scan")
    hook += b"\x8A\x42\x03\x3C\x30"  # final digit >= '0'
    emit_short_jump(0x72, "manager_scan")
    hook += b"\x3C\x34"  # final digit <= '4'
    emit_short_jump(0x77, "manager_scan")
    hook += b"\x81\x7A\x04" + struct.pack("<I", 0x676D692E)  # ".img"
    emit_short_jump(0x75, "manager_scan")
    hook += b"\x80\x7A\x08\x00"
    emit_short_jump(0x75, "manager_scan")
    hook += b"\xFF\x05" + struct.pack("<I", image_7400_7404_address)
    hook += b"\x8B\x0D" + struct.pack("<I", CLIENT_LT_SURF_MANAGER_POINTER)
    target_redirect_jump_offset = len(hook)
    hook += b"\xE9\x00\x00\x00\x00"

    labels["manager_scan"] = len(hook)
    hook += b"\xB8" + struct.pack("<I", CLIENT_SHARD_SURF_MANAGER_ARRAY)
    hook += b"\xBA" + struct.pack("<I", CLIENT_SHARD_SURF_MANAGER_COUNT)
    labels["manager_loop"] = len(hook)
    hook += b"\x3B\x08"  # cmp ecx,[eax]
    emit_short_jump(0x74, "shard_manager")  # je
    hook += b"\x83\xC0\x04\x4A"  # next pointer; dec edx
    emit_short_jump(0x75, "manager_loop")  # jnz
    passthrough_jump_offset = len(hook)
    hook += b"\xE9\x00\x00\x00\x00"
    labels["shard_manager"] = len(hook)
    hook += b"\x51"  # preserve the original SpriteXX manager
    hook += b"\x8B\x54\x24\x08\x85\xD2"  # filename; null guard
    emit_short_jump(0x75, "have_filename")  # jnz
    hook += b"\x59"
    null_jump_offset = len(hook)
    hook += b"\xE9\x00\x00\x00\x00"
    labels["have_filename"] = len(hook)
    # The selected 3.81C mixed client contains newer 400c.img..404c.img
    # tooltip surfaces in SpriteXX shards.  LiTo asks the base Surf manager
    # for numeric 400..405 toolbar resources; accepting those c aliases turns
    # the intended 19x21 hit boxes into overlapping 149x65 rectangles.  Reject
    # only the exact 400c.img..405c.img shard names so native lookup continues
    # to Sprite.idx's byte-identical plain 400.img..405.img records.
    hook += b"\x66\x81\x3A" + struct.pack("<H", 0x3034)  # "40"
    emit_short_jump(0x75, "probe_lt_collision")
    hook += b"\x8A\x42\x02\x3C\x30"  # third digit >= '0'
    emit_short_jump(0x72, "probe_lt_collision")
    hook += b"\x3C\x35"  # third digit <= '5'
    emit_short_jump(0x77, "probe_lt_collision")
    hook += b"\x80\x7A\x03\x63"  # exact lowercase 'c'
    emit_short_jump(0x75, "probe_lt_collision")
    hook += b"\x81\x7A\x04" + struct.pack("<I", 0x676D692E)  # ".img"
    emit_short_jump(0x75, "probe_lt_collision")
    hook += b"\x80\x7A\x08\x00"
    emit_short_jump(0x75, "probe_lt_collision")
    hook += b"\xFF\x05" + struct.pack("<I", image_400c_405c_address)
    hook += b"\x59\x33\xC0\xC2\x0C\x00"  # restore manager; not found
    labels["probe_lt_collision"] = len(hook)
    hook += b"\xFF\x05" + struct.pack("<I", attempts_address)
    # One saved manager precedes the original return address, so arg8 starts at
    # [esp+0x10].  After each push, the next original argument stays there.
    hook += b"\xFF\x74\x24\x10" * 3
    hook += b"\x8B\x0D" + struct.pack("<I", CLIENT_LT_SURF_MANAGER_POINTER)
    call_offset = len(hook)
    hook += b"\xE8\x00\x00\x00\x00"
    hook += b"\x85\xC0"
    emit_short_jump(0x75, "lt_collision")  # jnz
    hook += b"\xFF\x05" + struct.pack("<I", misses_address)
    hook += b"\x59"  # restore original SpriteXX manager
    fallback_jump_offset = len(hook)
    hook += b"\xE9\x00\x00\x00\x00"
    labels["lt_collision"] = len(hook)
    hook += b"\xFF\x05" + struct.pack("<I", hits_address)
    hook += b"\x8B\x54\x24\x08"  # original filename
    hook += b"\x81\x3A" + struct.pack("<I", 0x37353236)  # "6257"
    emit_short_jump(0x75, "check_6258")
    hook += b"\x81\x7A\x04" + struct.pack("<I", 0x676D692E)  # ".img"
    emit_short_jump(0x75, "free_lt_probe")
    hook += b"\x80\x7A\x08\x00"
    emit_short_jump(0x75, "free_lt_probe")
    hook += b"\xFF\x05" + struct.pack("<I", image_6257_address)
    emit_short_jump(0xEB, "free_lt_probe")
    labels["check_6258"] = len(hook)
    hook += b"\x81\x3A" + struct.pack("<I", 0x38353236)  # "6258"
    emit_short_jump(0x75, "free_lt_probe")
    hook += b"\x81\x7A\x04" + struct.pack("<I", 0x676D692E)  # ".img"
    emit_short_jump(0x75, "free_lt_probe")
    hook += b"\x80\x7A\x08\x00"
    emit_short_jump(0x75, "free_lt_probe")
    hook += b"\xFF\x05" + struct.pack("<I", image_6258_address)
    labels["free_lt_probe"] = len(hook)
    hook += b"\x50\xE8" + struct.pack(
        "<i", CLIENT_FREE - (hook_address + len(hook) + 6)
    )
    hook += b"\x83\xC4\x04\x59\x33\xC0\xC2\x0C\x00"
    labels["passthrough"] = len(hook)

    for displacement_offset, label in fixups:
        displacement = labels[label] - (displacement_offset + 1)
        if not -128 <= displacement <= 127:
            raise RuntimeError("LTsurf hook short jump is out of range")
        hook[displacement_offset] = displacement & 0xFF
    struct.pack_into(
        "<i",
        hook,
        call_offset + 1,
        trampoline_address - (hook_address + call_offset + 5),
    )
    for jump_offset in (
        target_redirect_jump_offset,
        fallback_jump_offset,
        null_jump_offset,
        passthrough_jump_offset,
    ):
        struct.pack_into(
            "<i",
            hook,
            jump_offset + 1,
            trampoline_address - (hook_address + jump_offset + 5),
        )

    if len(hook) > 0x1C0:
        raise RuntimeError("LTsurf precedence hook overlaps its trampoline")
    page = bytearray(0x25C)
    page[: len(hook)] = hook
    page[0x1C0 : 0x1C0 + len(trampoline)] = trampoline
    if write_process(process_handle, remote, bytes(page)) != len(page):
        raise RuntimeError("short write while installing LTsurf precedence hook")
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(remote), len(page)
    ):
        raise win_error("FlushInstructionCache(LTsurf precedence)")

    entry_patch = b"\xE9" + struct.pack(
        "<i", hook_address - (CLIENT_RESOURCE_OPEN + 5)
    ) + b"\x90"
    write_process_protected(process_handle, CLIENT_RESOURCE_OPEN, entry_patch)
    return {
        "status": "installed",
        "target": f"0x{CLIENT_RESOURCE_OPEN:08X}",
        "target_original": current.hex(" ").upper(),
        "target_patch": entry_patch.hex(" ").upper(),
        "hook_address": f"0x{hook_address:08X}",
        "trampoline_address": f"0x{trampoline_address:08X}",
        "telemetry_address": f"0x{telemetry_address:08X}",
        "policy": (
            "skip newer SpriteXX entry when LTsurf has the same name; "
            "route the verified base-Surf 7400..7404 aliases to LTsurf; "
            "reject exact SpriteXX 400c.img..405c.img aliases so the native "
            "19x21 plain toolbar is retained; preserve all other native "
            "base-Surf then LTsurf fallbacks"
        ),
        "executable_modified": False,
        "pak_files_modified": False,
    }


def update_lito_surface_precedence(
    process_handle: int, state: dict[str, Any]
) -> None:
    raw = read_process(
        process_handle, int(state["telemetry_address"], 16), 28
    )
    if len(raw) != 28:
        return
    (attempts, hits, misses, image_6257, image_6258, image_7400_7404,
     image_400c_405c) = struct.unpack("<7I", raw)
    state["telemetry"] = {
        "sprite_shard_requests_probed": attempts,
        "newer_shard_collisions_bypassed": hits,
        "ordinary_shard_requests_preserved": misses,
        "6257_img_collisions_bypassed": image_6257,
        "6258_img_collisions_bypassed": image_6258,
        "7400_7404_base_aliases_redirected": image_7400_7404,
        "400c_405c_shard_aliases_bypassed": image_400c_405c,
    }


def install_lobby_overlay_null_guard(
    process_handle: int,
    native_arena_transition: bool = True,
    preserve_native_character_selection: bool = False,
) -> dict[str, Any] | None:
    """Keep native lobby callbacks safe until world scenes exist.

    LiTo's Tournament UI can activate while the ordinary Lineage world scene
    is still null.  sub_469230 dereferences that optional scene without a
    guard, producing the repeatable 0x0046931D access violation on modern
    machines.  Painting still uses a trampoline, but sub_468FF0's input path
    receives surgical null-branch patches so its native toolbar hit testing is
    preserved.  The protected executable on disk is never modified.
    """

    overlay_current = read_process(
        process_handle,
        CLIENT_LOBBY_OVERLAY_CALLBACK,
        len(CLIENT_LOBBY_OVERLAY_CALLBACK_PROLOGUE),
    )
    hit_test_current = read_process(
        process_handle,
        CLIENT_LOBBY_HIT_TEST_CALLBACK,
        len(CLIENT_LOBBY_HIT_TEST_CALLBACK_PROLOGUE),
    )
    world_shop_current = read_process(
        process_handle,
        CLIENT_WORLD_SHOP_CALLBACK,
        len(CLIENT_WORLD_SHOP_CALLBACK_PROLOGUE),
    )
    ready_handler_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_READY_HANDLER,
        len(CLIENT_TOURNAMENT_READY_HANDLER_PROLOGUE),
    )
    start_handler_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_START_HANDLER,
        len(CLIENT_TOURNAMENT_START_HANDLER_PROLOGUE),
    )
    ready_world_call_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_READY_WORLD_CALL,
        len(CLIENT_TOURNAMENT_READY_WORLD_CALL_BYTES),
    )
    bootstrap_selection_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES),
    )
    bootstrap_select_send_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES),
    )
    ready_dispatch_call_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_READY_DISPATCH_CALL,
        len(CLIENT_TOURNAMENT_READY_DISPATCH_CALL_BYTES),
    )
    native_addr_case_current = read_process(
        process_handle,
        CLIENT_NATIVE_ADDR_CASE,
        len(CLIENT_NATIVE_ADDR_CASE_BYTES),
    )
    native_addr_send_call_current = read_process(
        process_handle,
        CLIENT_NATIVE_ADDR_SEND_CALL,
        len(CLIENT_NATIVE_ADDR_SEND_CALL_BYTES),
    )
    countdown_dispatch_call_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL,
        len(CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL_BYTES),
    )
    if (
        overlay_current != CLIENT_LOBBY_OVERLAY_CALLBACK_PROLOGUE
        or hit_test_current != CLIENT_LOBBY_HIT_TEST_CALLBACK_PROLOGUE
        or world_shop_current != CLIENT_WORLD_SHOP_CALLBACK_PROLOGUE
        or ready_handler_current != CLIENT_TOURNAMENT_READY_HANDLER_PROLOGUE
        or start_handler_current != CLIENT_TOURNAMENT_START_HANDLER_PROLOGUE
        or ready_world_call_current != CLIENT_TOURNAMENT_READY_WORLD_CALL_BYTES
        or bootstrap_selection_current != CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES
        or bootstrap_select_send_current != CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES
        or ready_dispatch_call_current != CLIENT_TOURNAMENT_READY_DISPATCH_CALL_BYTES
        or native_addr_case_current != CLIENT_NATIVE_ADDR_CASE_BYTES
        or native_addr_send_call_current != CLIENT_NATIVE_ADDR_SEND_CALL_BYTES
        or countdown_dispatch_call_current
        != CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL_BYTES
    ):
        return None
    hit_test_patch_state: list[dict[str, str]] = []
    for address, expected, replacement in CLIENT_LOBBY_HIT_TEST_NULL_PATCHES:
        current = read_process(process_handle, address, len(expected))
        if current not in (expected, replacement):
            return None
        if current == expected:
            write_process_protected(process_handle, address, replacement)
        hit_test_patch_state.append(
            {
                "address": f"0x{address:08X}",
                "original": expected.hex().upper(),
                "replacement": replacement.hex().upper(),
            }
        )

    def install_guard(
        callback: int, prologue: bytes, null_return: bytes
    ) -> tuple[int, bytes]:
        remote = int(
            kernel32.VirtualAllocEx(
                process_handle,
                None,
                0x1000,
                MEM_COMMIT | MEM_RESERVE,
                PAGE_EXECUTE_READWRITE,
            )
            or 0
        )
        if not remote or remote > 0xFFFFFFFF:
            raise RuntimeError("could not allocate an x86 lobby callback guard")

        stub = bytearray()
        jump_to_return_fixups: list[int] = []
        stub += b"\xA1" + struct.pack("<I", CLIENT_WORLD_SCENE)
        stub += b"\x85\xC0"
        stub += b"\x74\x00"
        jump_to_return_fixups.append(len(stub) - 1)
        stub += b"\xA1" + struct.pack("<I", CLIENT_STATIC_SCENE)
        stub += b"\x85\xC0"
        stub += b"\x74\x00"
        jump_to_return_fixups.append(len(stub) - 1)
        stub += prologue
        stub += b"\xE9" + struct.pack(
            "<i", callback + len(prologue) - (remote + len(stub) + 5)
        )
        return_offset = len(stub)
        stub += null_return
        for displacement_index in jump_to_return_fixups:
            displacement = return_offset - (displacement_index + 1)
            if not -128 <= displacement <= 127:
                raise RuntimeError("lobby callback guard branch exceeded rel8 range")
            stub[displacement_index] = displacement & 0xFF

        written = write_process(process_handle, remote, bytes(stub))
        if written != len(stub):
            raise RuntimeError("short write while installing lobby callback guard")
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(remote), len(stub)
        ):
            raise win_error("FlushInstructionCache(lobby callback guard)")

        entry_patch = b"\xE9" + struct.pack("<i", remote - (callback + 5))
        write_process_protected(process_handle, callback, entry_patch)
        return remote, entry_patch

    overlay_remote, overlay_entry_patch = install_guard(
        CLIENT_LOBBY_OVERLAY_CALLBACK,
        CLIENT_LOBBY_OVERLAY_CALLBACK_PROLOGUE,
        b"\xC3",
    )
    world_shop_remote, world_shop_entry_patch = install_guard(
        CLIENT_WORLD_SHOP_CALLBACK,
        CLIENT_WORLD_SHOP_CALLBACK_PROLOGUE,
        b"\xC3",
    )
    ready_remote: int | None = None
    ready_entry_patch: bytes | None = None
    ready_world_bridge_remote: int | None = None
    ready_world_call_patch: bytes | None = None
    start_remote: int | None = None
    start_entry_patch: bytes | None = None
    selection_patch: bytes | None = None
    if native_arena_transition:
        if not preserve_native_character_selection:
            selection_patch = (
                b"\xBB" + struct.pack("<I", CLIENT_TOURNAMENT_BOOTSTRAP_RECORD)
                + b"\xE9" + struct.pack(
                    "<i",
                    0x0049293E - (CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION + 10),
                )
            )
            selection_patch += b"\x90" * (
                len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES) - len(selection_patch)
            )
            write_process_protected(
                process_handle,
                CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION,
                selection_patch,
            )
            write_process_protected(
                process_handle,
                CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND,
                b"\x90" * len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES),
            )
        ready_world_bridge_remote = int(
            kernel32.VirtualAllocEx(
                process_handle,
                None,
                0x1000,
                MEM_COMMIT | MEM_RESERVE,
                PAGE_EXECUTE_READWRITE,
            )
            or 0
        )
        if not ready_world_bridge_remote or ready_world_bridge_remote > 0xFFFFFFFF:
            raise RuntimeError("could not allocate the Tournament world bootstrap bridge")

        marker_address = ready_world_bridge_remote + 0x200
        root_snapshot_address = ready_world_bridge_remote + 0x204
        controller_snapshot_address = ready_world_bridge_remote + 0x208
        ready_handler_return_marker_address = ready_world_bridge_remote + 0x20C
        native_addr_stage_marker_address = ready_world_bridge_remote + 0x210
        countdown_stage_marker_address = ready_world_bridge_remote + 0x214
        ready_handler_thread_id_address = ready_world_bridge_remote + 0x218

        def mark(stage: int) -> None:
            bridge.extend(b"\xC7\x05")
            bridge.extend(struct.pack("<I", marker_address))
            bridge.extend(struct.pack("<I", stage))

        def call_relative(target: int) -> None:
            bridge.extend(b"\xE8")
            bridge.extend(
                struct.pack(
                    "<i",
                    target - (ready_world_bridge_remote + len(bridge) + 4),
                )
            )

        bridge = bytearray()
        bridge_labels: dict[str, int] = {}
        bridge_fixups: list[tuple[int, str]] = []

        def jump_near(opcode: bytes, label: str) -> None:
            bridge.extend(opcode)
            bridge_fixups.append((len(bridge), label))
            bridge.extend(b"\x00\x00\x00\x00")

        mark(1)  # entered the patched native S_D1 READY call site
        bridge += b"\xA1" + struct.pack("<I", CLIENT_TOURNAMENT_WORLD_ROOT)
        bridge += b"\x85\xC0"
        jump_near(b"\x0F\x85", "root_ready")
        mark(2)  # the standalone native flow omitted the classic character handoff
        call_relative(CLIENT_TOURNAMENT_WORLD_BOOTSTRAP)
        mark(3)  # sub_4928C0 returned inside the native S_D1 callback frame
        bridge += b"\xA1" + struct.pack(
            "<I", CLIENT_TOURNAMENT_BOOTSTRAP_RECORD + 0x40
        )
        bridge += b"\xA3" + struct.pack("<I", CLIENT_LOCAL_OBJECT_ID)

        bridge_labels["root_ready"] = len(bridge)
        bridge += b"\xA1" + struct.pack("<I", CLIENT_TOURNAMENT_WORLD_ROOT)
        bridge += b"\xA3" + struct.pack("<I", root_snapshot_address)
        bridge += b"\x85\xC0"
        jump_near(b"\x0F\x84", "null_root")
        bridge += b"\xA1" + struct.pack(
            "<I", CLIENT_TOURNAMENT_READY_WORLD_CONTROLLER
        )
        bridge += b"\xA3" + struct.pack("<I", controller_snapshot_address)
        bridge += b"\x85\xC0"
        jump_near(b"\x0F\x84", "construct_world")
        mark(4)  # native root and controller exist: take sub_47E360 branch
        call_relative(CLIENT_TOURNAMENT_READY_WORLD_REFRESH)
        jump_near(b"\xE9", "transition_done")
        bridge_labels["construct_world"] = len(bridge)
        mark(5)  # native controller is absent: take sub_47CF00 constructor branch
        call_relative(CLIENT_TOURNAMENT_READY_WORLD_CONSTRUCTOR)
        bridge_labels["transition_done"] = len(bridge)
        mark(6)  # native READY world transition returned
        bridge += b"\xC3"
        bridge_labels["null_root"] = len(bridge)
        mark(7)  # native selection did not construct the required world root
        bridge += b"\xC3"
        for displacement_offset, label in bridge_fixups:
            struct.pack_into(
                "<i",
                bridge,
                displacement_offset,
                bridge_labels[label] - (displacement_offset + 4),
            )
        if write_process(process_handle, ready_world_bridge_remote, bytes(bridge)) != len(bridge):
            raise RuntimeError("short write while installing Tournament world bootstrap bridge")
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(ready_world_bridge_remote), len(bridge)
        ):
            raise win_error("FlushInstructionCache(Tournament world bootstrap bridge)")

        ready_dispatch_wrapper = ready_world_bridge_remote + 0x300
        ready_dispatch_stub = bytearray(b"\xE8")
        ready_dispatch_stub += struct.pack(
            "<i", CLIENT_TOURNAMENT_READY_HANDLER - (ready_dispatch_wrapper + 5)
        )
        ready_dispatch_stub += b"\x50\x64\xA1\x24\x00\x00\x00"
        ready_dispatch_stub += b"\xA3" + struct.pack(
            "<I", ready_handler_thread_id_address
        )
        ready_dispatch_stub += b"\x58"
        ready_dispatch_stub += b"\xC7\x05" + struct.pack(
            "<I", ready_handler_return_marker_address
        ) + struct.pack("<I", 1)
        ready_dispatch_stub += b"\xC3"

        native_addr_case_trampoline = ready_world_bridge_remote + 0x340
        native_addr_case_stub = bytearray(b"\xC7\x05")
        native_addr_case_stub += struct.pack("<I", native_addr_stage_marker_address)
        native_addr_case_stub += struct.pack("<I", 1)
        native_addr_case_stub += CLIENT_NATIVE_ADDR_CASE_BYTES
        native_addr_case_stub += b"\xE9" + struct.pack(
            "<i",
            (CLIENT_NATIVE_ADDR_CASE + len(CLIENT_NATIVE_ADDR_CASE_BYTES))
            - (native_addr_case_trampoline + len(native_addr_case_stub) + 5),
        )

        native_addr_send_wrapper = ready_world_bridge_remote + 0x380
        native_addr_send_stub = bytearray(b"\xC7\x05")
        native_addr_send_stub += struct.pack("<I", native_addr_stage_marker_address)
        native_addr_send_stub += struct.pack("<I", 2)
        # Preserve the native cddd vararg frame: copy edx/eax/ebx/opcode/format
        # in reverse order before nesting the serializer call.
        native_addr_send_stub += b"\xFF\x74\x24\x14" * 5
        native_addr_send_stub += b"\xE8" + struct.pack(
            "<i",
            CLIENT_NATIVE_PACKET_SEND
            - (native_addr_send_wrapper + len(native_addr_send_stub) + 5),
        )
        native_addr_send_stub += b"\x83\xC4\x14"
        native_addr_send_stub += b"\xC7\x05" + struct.pack(
            "<I", native_addr_stage_marker_address
        ) + struct.pack("<I", 3)
        native_addr_send_stub += b"\xC3"

        countdown_dispatch_wrapper = ready_world_bridge_remote + 0x3C0
        countdown_dispatch_stub = bytearray(b"\xC7\x05")
        countdown_dispatch_stub += struct.pack("<I", countdown_stage_marker_address)
        countdown_dispatch_stub += struct.pack("<I", 1)
        countdown_dispatch_stub += b"\xE8" + struct.pack(
            "<i",
            CLIENT_TOURNAMENT_COUNTDOWN_HANDLER
            - (countdown_dispatch_wrapper + len(countdown_dispatch_stub) + 5),
        )
        countdown_dispatch_stub += b"\xC7\x05" + struct.pack(
            "<I", countdown_stage_marker_address
        ) + struct.pack("<I", 2)
        countdown_dispatch_stub += b"\xC3"

        for address, stub, label in (
            (ready_dispatch_wrapper, ready_dispatch_stub, "S_D1 dispatch wrapper"),
            (native_addr_case_trampoline, native_addr_case_stub, "C_ADDR case marker"),
            (native_addr_send_wrapper, native_addr_send_stub, "C_ADDR send wrapper"),
            (countdown_dispatch_wrapper, countdown_dispatch_stub, "S_E1 countdown wrapper"),
        ):
            if write_process(process_handle, address, bytes(stub)) != len(stub):
                raise RuntimeError(f"short write while installing {label}")
            if not kernel32.FlushInstructionCache(
                process_handle, ctypes.c_void_p(address), len(stub)
            ):
                raise win_error(f"FlushInstructionCache({label})")

        ready_dispatch_call_patch = b"\xE8" + struct.pack(
            "<i",
            ready_dispatch_wrapper - (CLIENT_TOURNAMENT_READY_DISPATCH_CALL + 5),
        )
        native_addr_case_patch = b"\xE9" + struct.pack(
            "<i", native_addr_case_trampoline - (CLIENT_NATIVE_ADDR_CASE + 5)
        )
        native_addr_send_call_patch = b"\xE8" + struct.pack(
            "<i", native_addr_send_wrapper - (CLIENT_NATIVE_ADDR_SEND_CALL + 5)
        )
        countdown_dispatch_call_patch = b"\xE8" + struct.pack(
            "<i",
            countdown_dispatch_wrapper
            - (CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL + 5),
        )
        write_process_protected(
            process_handle,
            CLIENT_TOURNAMENT_READY_DISPATCH_CALL,
            ready_dispatch_call_patch,
        )
        write_process_protected(
            process_handle, CLIENT_NATIVE_ADDR_CASE, native_addr_case_patch
        )
        write_process_protected(
            process_handle,
            CLIENT_NATIVE_ADDR_SEND_CALL,
            native_addr_send_call_patch,
        )
        write_process_protected(
            process_handle,
            CLIENT_TOURNAMENT_COUNTDOWN_DISPATCH_CALL,
            countdown_dispatch_call_patch,
        )
        ready_world_call_patch = b"\xE8" + struct.pack(
            "<i",
            ready_world_bridge_remote - (CLIENT_TOURNAMENT_READY_WORLD_CALL + 5),
        )
        write_process_protected(
            process_handle,
            CLIENT_TOURNAMENT_READY_WORLD_CALL,
            ready_world_call_patch,
        )
    else:
        ready_remote = int(
            kernel32.VirtualAllocEx(
                process_handle,
                None,
                0x1000,
                MEM_COMMIT | MEM_RESERVE,
                PAGE_EXECUTE_READWRITE,
            )
            or 0
        )
        if not ready_remote or ready_remote > 0xFFFFFFFF:
            raise RuntimeError("could not allocate the Tournament ready-state guard")
        # Legacy phase-only fallback. It is retained solely for diagnostics;
        # normal runs must execute the native ready handler because that path
        # creates dword_64B908 and the arena controls consumed by S_D2.
        ready_stub = bytes.fromhex(
            "8D 41 01 "      # lea eax,[ecx+1]
            "80 38 00 "      # scan: cmp byte ptr [eax],0
            "74 03 "         # je found
            "40 "            # inc eax
            "EB F8 "         # jmp scan
            "80 78 01 00 "   # found: cmp byte ptr [eax+1],0
            "74 15 "         # je cancelled
            "C7 05 98 AF 64 00 03 00 00 00 "
            "C7 05 D4 BB 64 00 04 00 00 00 "
            "C3 "
            "C7 05 98 AF 64 00 02 00 00 00 "
            "C7 05 D4 BB 64 00 03 00 00 00 "
            "C3"
        )
        if write_process(process_handle, ready_remote, ready_stub) != len(ready_stub):
            raise RuntimeError("short write while installing Tournament ready-state guard")
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(ready_remote), len(ready_stub)
        ):
            raise win_error("FlushInstructionCache(Tournament ready-state guard)")
        ready_entry_patch = b"\xE9" + struct.pack(
            "<i", ready_remote - (CLIENT_TOURNAMENT_READY_HANDLER + 5)
        )
        write_process_protected(
            process_handle, CLIENT_TOURNAMENT_READY_HANDLER, ready_entry_patch
        )

        start_remote = int(
            kernel32.VirtualAllocEx(
                process_handle,
                None,
                0x1000,
                MEM_COMMIT | MEM_RESERVE,
                PAGE_EXECUTE_READWRITE,
            )
            or 0
        )
        if not start_remote or start_remote > 0xFFFFFFFF:
            raise RuntimeError("could not allocate the Tournament start-state guard")
        # Compatibility fallback for clients whose standalone handoff still
        # lacks one of the native Tournament arena controller objects.
        start_stub = bytearray.fromhex(
            "C7 05 98 AF 64 00 04 00 00 00 "
            "C7 05 D4 BB 64 00 04 00 00 00 "
            "C3"
        )
        if write_process(process_handle, start_remote, bytes(start_stub)) != len(start_stub):
            raise RuntimeError("short write while installing Tournament start-state guard")
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(start_remote), len(start_stub)
        ):
            raise win_error("FlushInstructionCache(Tournament start-state guard)")
        start_entry_patch = b"\xE9" + struct.pack(
            "<i", start_remote - (CLIENT_TOURNAMENT_START_HANDLER + 5)
        )
        write_process_protected(
            process_handle, CLIENT_TOURNAMENT_START_HANDLER, start_entry_patch
        )
    return {
        "status": "installed",
        "world_scene_pointer": f"0x{CLIENT_WORLD_SCENE:08X}",
        "static_scene_pointer": f"0x{CLIENT_STATIC_SCENE:08X}",
        "callbacks": [
            {
                "name": "overlay_paint",
                "callback": f"0x{CLIENT_LOBBY_OVERLAY_CALLBACK:08X}",
                "trampoline": f"0x{overlay_remote:08X}",
                "original": overlay_current.hex().upper(),
                "entry_patch": overlay_entry_patch.hex().upper(),
            },
            {
                "name": "overlay_hit_test",
                "callback": f"0x{CLIENT_LOBBY_HIT_TEST_CALLBACK:08X}",
                "trampoline": None,
                "original": hit_test_current.hex().upper(),
                "entry_patch": None,
                "null_branch_patches": hit_test_patch_state,
                "native_hit_testing_preserved": True,
            },
            {
                "name": "world_shop_mouse_callback",
                "callback": f"0x{CLIENT_WORLD_SHOP_CALLBACK:08X}",
                "trampoline": f"0x{world_shop_remote:08X}",
                "original": world_shop_current.hex().upper(),
                "entry_patch": world_shop_entry_patch.hex().upper(),
            },
            {
                "name": "tournament_ready_state",
                "callback": f"0x{CLIENT_TOURNAMENT_READY_HANDLER:08X}",
                "trampoline": (
                    f"0x{ready_remote:08X}"
                    if ready_remote
                    else (
                        f"0x{ready_world_bridge_remote:08X}"
                        if ready_world_bridge_remote
                        else None
                    )
                ),
                "original": ready_handler_current.hex().upper(),
                "entry_patch": ready_entry_patch.hex().upper() if ready_entry_patch else None,
                "world_bootstrap_call": f"0x{CLIENT_TOURNAMENT_WORLD_BOOTSTRAP:08X}",
                "world_bootstrap_root": f"0x{CLIENT_TOURNAMENT_WORLD_ROOT:08X}",
                "late_world_bootstrap_disabled": (
                    not native_arena_transition
                ),
                "null_world_root_returns_without_constructor": (
                    False
                ),
                "bootstrap_record": f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_RECORD:08X}",
                "local_object_id": f"0x{CLIENT_LOCAL_OBJECT_ID:08X}",
                "bootstrap_selection_patch": (
                    selection_patch.hex().upper() if selection_patch else None
                ),
                "duplicate_character_select_suppressed": (
                    native_arena_transition
                    and not preserve_native_character_selection
                ),
                "native_character_selection_preserved": (
                    native_arena_transition
                    and preserve_native_character_selection
                ),
                "world_constructor_call_patch": (
                    ready_world_call_patch.hex().upper()
                    if ready_world_call_patch
                    else None
                ),
                "world_transition_marker_address": (
                    f"0x{marker_address:08X}" if native_arena_transition else None
                ),
                "world_transition_root_snapshot_address": (
                    f"0x{root_snapshot_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "world_transition_controller_snapshot_address": (
                    f"0x{controller_snapshot_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "world_refresh_call": (
                    f"0x{CLIENT_TOURNAMENT_READY_WORLD_REFRESH:08X}"
                    if native_arena_transition
                    else None
                ),
                "ready_handler_return_marker_address": (
                    f"0x{ready_handler_return_marker_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "ready_handler_thread_id_address": (
                    f"0x{ready_handler_thread_id_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "native_addr_stage_marker_address": (
                    f"0x{native_addr_stage_marker_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "countdown_stage_marker_address": (
                    f"0x{countdown_stage_marker_address:08X}"
                    if native_arena_transition
                    else None
                ),
                "ready_dispatch_call_patch": (
                    ready_dispatch_call_patch.hex().upper()
                    if native_arena_transition
                    else None
                ),
                "native_addr_case_patch": (
                    native_addr_case_patch.hex().upper()
                    if native_arena_transition
                    else None
                ),
                "native_addr_send_call_patch": (
                    native_addr_send_call_patch.hex().upper()
                    if native_arena_transition
                    else None
                ),
                "countdown_dispatch_call_patch": (
                    countdown_dispatch_call_patch.hex().upper()
                    if native_arena_transition
                    else None
                ),
                "native_launcher_ui_transition_bypassed": not native_arena_transition,
                "native_transition_enabled": native_arena_transition,
            },
            {
                "name": "tournament_start_state",
                "callback": f"0x{CLIENT_TOURNAMENT_START_HANDLER:08X}",
                "trampoline": f"0x{start_remote:08X}" if start_remote else None,
                "original": start_handler_current.hex().upper(),
                "entry_patch": start_entry_patch.hex().upper() if start_entry_patch else None,
                "native_launcher_arena_transition_bypassed": not native_arena_transition,
                "native_transition_enabled": native_arena_transition,
            },
        ],
        "executable_modified": False,
    }


def install_tournament_selection_bridge(process_handle: int) -> dict[str, Any]:
    """Arm the compact record only after native S_CB/C_C9 has completed."""

    selection_patch = (
        b"\xBB" + struct.pack("<I", CLIENT_TOURNAMENT_BOOTSTRAP_RECORD)
        + b"\xE9" + struct.pack(
            "<i",
            0x0049293E - (CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION + 10),
        )
    )
    selection_patch += b"\x90" * (
        len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES) - len(selection_patch)
    )
    send_patch = b"\x90" * len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES)
    selection_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES),
    )
    send_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES),
    )
    ready_direct_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_READY_DIRECT_CALL,
        len(CLIENT_TOURNAMENT_READY_CALL_BYTES),
    )
    ready_confirm_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_READY_CONFIRM_CALL,
        len(CLIENT_TOURNAMENT_READY_CONFIRM_CALL_BYTES),
    )
    bootstrap_full_tail_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL_BYTES),
    )
    bootstrap_full_entry_current = read_process(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY,
        len(CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY_BYTES),
    )
    if selection_current not in (
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES,
        selection_patch,
    ) or send_current not in (
        CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES,
        send_patch,
    ):
        raise RuntimeError("Tournament selection bridge signature changed")
    if ready_direct_current != CLIENT_TOURNAMENT_READY_CALL_BYTES:
        raise RuntimeError("Tournament direct Ready call signature changed")
    if ready_confirm_current != CLIENT_TOURNAMENT_READY_CONFIRM_CALL_BYTES:
        raise RuntimeError("Tournament confirmed Ready call signature changed")
    if bootstrap_full_tail_current != CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL_BYTES:
        raise RuntimeError("Tournament bootstrap full-tail signature changed")
    if bootstrap_full_entry_current != CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY_BYTES:
        raise RuntimeError("Tournament bootstrap full-entry signature changed")

    start_bridge_remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not start_bridge_remote or start_bridge_remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate the Tournament Start bootstrap bridge")
    minimal_bootstrap_return_address = start_bridge_remote + 0x300
    minimal_bootstrap_return = bytes.fromhex(
        "8B 4D F4 64 89 0D 00 00 00 00 5F 5E 5B 8B E5 5D C3"
    )
    if write_process(
        process_handle,
        minimal_bootstrap_return_address,
        minimal_bootstrap_return,
    ) != len(minimal_bootstrap_return):
        raise RuntimeError("short write while installing minimal bootstrap return")
    if not kernel32.FlushInstructionCache(
        process_handle,
        ctypes.c_void_p(start_bridge_remote),
        0x1000,
    ):
        raise win_error("FlushInstructionCache(Tournament Start bootstrap bridge)")
    if selection_current == CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES:
        write_process_protected(
            process_handle,
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION,
            selection_patch,
        )
    if send_current == CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES:
        write_process_protected(
            process_handle,
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND,
            send_patch,
        )
    bootstrap_tail_patch = b"\xE9" + struct.pack(
        "<i",
        minimal_bootstrap_return_address
        - (CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL + 5),
    )
    write_process_protected(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL,
        bootstrap_tail_patch,
    )
    bootstrap_entry_patch = (
        b"\xBB"
        + struct.pack("<I", CLIENT_TOURNAMENT_BOOTSTRAP_RECORD)
        + b"\xBE\xFF\xFF\xFF\xFF"
        + b"\xE9"
        + struct.pack(
            "<i",
            0x00492C26
            - (
                CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY
                + len(CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY_BYTES)
            ),
        )
    )
    write_process_protected(
        process_handle,
        CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY,
        bootstrap_entry_patch,
    )
    return {
        "status": "installed_after_native_character_select",
        "selection_address": f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION:08X}",
        "selection_patch": selection_patch.hex().upper(),
        "duplicate_select_address": (
            f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND:08X}"
        ),
        "duplicate_select_patch": send_patch.hex().upper(),
        "native_s_cb_c_c9_preserved_before_install": True,
        "ready_direct_call": f"0x{CLIENT_TOURNAMENT_READY_DIRECT_CALL:08X}",
        "ready_direct_original": ready_direct_current.hex().upper(),
        "ready_direct_preserved": True,
        "ready_confirm_call": f"0x{CLIENT_TOURNAMENT_READY_CONFIRM_CALL:08X}",
        "ready_confirm_original": ready_confirm_current.hex().upper(),
        "ready_confirm_preserved": True,
        "native_ready_send_calls_modified": False,
        "bootstrap_full_tail": (
            f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL:08X}"
        ),
        "bootstrap_full_tail_original": bootstrap_full_tail_current.hex().upper(),
        "bootstrap_full_tail_patch": bootstrap_tail_patch.hex().upper(),
        "minimal_bootstrap_return": f"0x{minimal_bootstrap_return_address:08X}",
        "full_selection_callback_reset_skipped": True,
        "bootstrap_full_entry": (
            f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY:08X}"
        ),
        "bootstrap_full_entry_original": bootstrap_full_entry_current.hex().upper(),
        "bootstrap_full_entry_patch": bootstrap_entry_patch.hex().upper(),
        "selection_mode_callback_preamble_skipped": True,
        "bootstrap_return_page": f"0x{start_bridge_remote:08X}",
        "bootstrap_timing": "native_s_d1_call_site_allocation_only",
        "executable_modified": False,
    }


def restore_tournament_selection_bridge(
    process_handle: int, state: dict[str, Any]
) -> dict[str, Any]:
    """Restore Confirm's native C_C9 path before a new character selection.

    The allocation-only world bootstrap deliberately reuses sub_4928C0 after
    the first character has entered Tournament.  It must not survive a native
    lobby-to-character return: page 7 uses that same function as its Confirm
    callback.  Restore every byte changed by the late bootstrap as one checked
    lifecycle operation, then release its private trampoline page.
    """

    patches = (
        (
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION,
            bytes.fromhex(str(state["selection_patch"])),
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECTION_BYTES,
            "selection lookup",
        ),
        (
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND,
            bytes.fromhex(str(state["duplicate_select_patch"])),
            CLIENT_TOURNAMENT_BOOTSTRAP_SELECT_SEND_BYTES,
            "C_C9 producer",
        ),
        (
            CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL,
            bytes.fromhex(str(state["bootstrap_full_tail_patch"])),
            CLIENT_TOURNAMENT_BOOTSTRAP_FULL_TAIL_BYTES,
            "full callback tail",
        ),
        (
            CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY,
            bytes.fromhex(str(state["bootstrap_full_entry_patch"])),
            CLIENT_TOURNAMENT_BOOTSTRAP_FULL_ENTRY_BYTES,
            "full callback entry",
        ),
    )
    restored: list[str] = []
    for address, installed, original, label in patches:
        current = read_process(process_handle, address, len(original))
        if current == original:
            restored.append(f"{label}:already-native")
            continue
        if current != installed:
            raise RuntimeError(
                f"Tournament selection bridge {label} changed before restore"
            )
        write_process_protected(process_handle, address, original)
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(address), len(original)
        ):
            raise win_error(f"FlushInstructionCache({label} restore)")
        restored.append(f"{label}:restored")

    allocation = int(str(state["bootstrap_return_page"]), 16)
    if allocation and not kernel32.VirtualFreeEx(
        process_handle, ctypes.c_void_p(allocation), 0, MEM_RELEASE
    ):
        raise win_error("VirtualFreeEx(Tournament selection bridge)")
    return {
        "status": "native_character_confirm_restored",
        "restored": restored,
        "released_bootstrap_page": f"0x{allocation:08X}",
        "native_c_c9_producer": f"0x{CLIENT_TOURNAMENT_WORLD_BOOTSTRAP:08X}",
        "executable_modified": False,
    }


def build_tournament_bootstrap_record(
    identity: bytes,
    profile: dict[str, Any] | None,
) -> bytes:
    """Build the native sub_4928C0 character record without logging text."""

    values = profile or {}
    record = bytearray(CLIENT_TOURNAMENT_BOOTSTRAP_RECORD_SIZE)
    clan = bytes(values.get("clan", b""))[:27]
    record[: min(len(identity), 27)] = identity[:27]
    record[0x1C : 0x1C + len(clan)] = clan
    record[0x2D] = int(values.get("type", 0)) & 0xFF
    record[0x2E] = int(values.get("sex", 0)) & 0xFF
    struct.pack_into("<H", record, 0x30, 0)
    struct.pack_into("<H", record, 0x32, int(values.get("max_hp", 50)) & 0xFFFF)
    struct.pack_into("<H", record, 0x34, int(values.get("max_mp", 10)) & 0xFFFF)
    record[0x36] = int(values.get("armor_class", 10)) & 0xFF
    record[0x37] = max(1, int(values.get("level", 1))) & 0xFF
    record[0x38] = int(values.get("strength", 13)) & 0xFF
    record[0x39] = int(values.get("constitution", 10)) & 0xFF
    record[0x3A] = int(values.get("dexterity", 10)) & 0xFF
    record[0x3B] = int(values.get("charisma", 9)) & 0xFF
    record[0x3C] = int(values.get("intelligence", 8)) & 0xFF
    record[0x3D] = int(values.get("wisdom", 11)) & 0xFF
    struct.pack_into("<I", record, 0x40, int(values.get("id", 1)) & 0xFFFFFFFF)
    struct.pack_into("<I", record, 0x4C, 32600)
    struct.pack_into("<I", record, 0x50, 32700)
    struct.pack_into("<I", record, 0x54, 4)
    return bytes(record)


def clone_tournament_bootstrap_record(
    identity: bytes,
    selected_record: bytes,
) -> bytes:
    """Clone the selected native S_CB row while changing only its display name."""

    if len(selected_record) != CLIENT_TOURNAMENT_BOOTSTRAP_RECORD_SIZE:
        raise ValueError("selected Tournament character record has the wrong size")
    if not identity or len(identity) >= 28:
        raise ValueError("Tournament bootstrap identity must be 1 to 27 bytes")
    record = bytearray(selected_record)
    record[:28] = b"\x00" * 28
    record[: len(identity)] = identity
    return bytes(record)


def consume_launcher_credentials() -> tuple[bytearray, bytearray, bytearray] | None:
    account = os.environ.pop("LITO381_ACCOUNT", None)
    password = os.environ.pop("LITO381_PASSWORD", None)
    character = os.environ.pop("LITO381_CHARACTER", "")
    if account is None and password is None:
        return None
    if account is None or password is None:
        raise ValueError("launcher must provide both account and password")
    try:
        account_bytes = bytearray(account.encode("cp950", errors="strict"))
        password_bytes = bytearray(password.encode("cp950", errors="strict"))
        character_bytes = bytearray(character.encode("cp950", errors="strict"))
    except UnicodeEncodeError as exc:
        raise ValueError("account/password contains text unsupported by the LiTo MS950 client") from exc
    if not 4 <= len(account_bytes) <= CLIENT_ACCOUNT_CELL_COUNT - 1:
        raise ValueError("LiTo account must be 4 to 27 MS950 bytes")
    if not 4 <= len(password_bytes) <= CLIENT_PASSWORD_CELL_COUNT - 1:
        raise ValueError("LiTo password must be 4 to 13 MS950 bytes")
    if len(character_bytes) > CLIENT_CHARACTER_NAME_CAPACITY - 1:
        raise ValueError("LiTo character name must be at most 27 MS950 bytes")
    # sub_4D3860, used by the native login button, uppercases the first byte.
    if 0x61 <= account_bytes[0] <= 0x7A:
        account_bytes[0] -= 0x20
    return account_bytes, password_bytes, character_bytes


def build_lito_display_identity(account: bytes, source_name: str) -> bytes:
    """Return the native 27-byte-safe ``ID(source)`` display identity."""

    source = source_name.strip()
    if not account or not source:
        return account
    prefix = bytes(account) + b"("
    suffix = b")"
    remaining = CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY - 1 - len(prefix) - len(suffix)
    if remaining <= 0:
        return bytes(account[: CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY - 1])
    encoded_source = bytearray()
    for character in source:
        encoded = character.encode("cp950", errors="strict")
        if len(encoded_source) + len(encoded) > remaining:
            break
        encoded_source.extend(encoded)
    if not encoded_source:
        return bytes(account[: CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY - 1])
    return prefix + bytes(encoded_source) + suffix


def legacy_vs_token_patch_required(
    *,
    launcher_credentials_supplied: bool,
    physical_native_login_session: bool,
) -> bool:
    """Whether the standalone launch lacks the official VS token state.

    Both launcher-injected and physically typed credentials authenticate the
    ordinary C_LOGIN.  Neither standalone route owns the legacy official
    launcher's token objects consumed by sub_46EF90 after that packet.
    """

    return launcher_credentials_supplied or physical_native_login_session


def credential_bootstrap_readiness(process_handle: int) -> dict[str, Any]:
    account_probe = read_process(process_handle, CLIENT_ACCOUNT_CELLS, 12)
    password_probe = read_process(process_handle, CLIENT_PASSWORD_CELLS, 12)
    return {
        "account_region_readable": len(account_probe) == 12,
        "password_region_readable": len(password_probe) == 12,
        "ready": len(account_probe) == 12 and len(password_probe) == 12,
    }


def install_credential_bootstrap(
    process_handle: int,
    account: bytearray,
    password: bytearray,
    character: bytearray,
    waiting_report: dict[str, Any] | None = None,
) -> dict[str, Any] | None:
    readiness = credential_bootstrap_readiness(process_handle)
    if waiting_report is not None:
        waiting_report["readiness"] = readiness
    if not readiness["ready"]:
        return None

    account_wire = bytes(account) + b"\x00"
    password_wire = bytes(password) + b"\x00"
    original_account = read_process(
        process_handle, CLIENT_ACCOUNT_CELLS, len(account_wire)
    )
    original_password = read_process(
        process_handle, CLIENT_PASSWORD_CELLS, len(password_wire)
    )
    if len(original_account) != len(account_wire) or len(original_password) != len(password_wire):
        return None

    # sub_4A33E0's launcher branch passes these addresses directly to the ordinary
    # `s` serializer (strlen + memcpy).  The official handoff therefore uses plain,
    # NUL-terminated bytes here, not the manual-login obfuscated character objects.
    write_process(process_handle, CLIENT_ACCOUNT_CELLS, account_wire)
    write_process(process_handle, CLIENT_PASSWORD_CELLS, password_wire)
    if character:
        write_process(
            process_handle,
            CLIENT_CHARACTER_NAME,
            bytes(character) + b"\x00",
        )
    write_process(process_handle, CLIENT_AUTO_LOGIN_FLAG, b"\x01")
    return {
        "status": "installed_for_process_lifetime",
        "account_length_bytes": len(account),
        "password_length_bytes": len(password),
        "character_name_supplied": bool(character),
        "account_address": f"0x{CLIENT_ACCOUNT_CELLS:08X}",
        "password_address": f"0x{CLIENT_PASSWORD_CELLS:08X}",
        "auto_login_flag": f"0x{CLIENT_AUTO_LOGIN_FLAG:08X}",
        "_restore_account": original_account.hex(),
        "_restore_password": original_password.hex(),
        "credentials_logged": False,
        "credential_memory_scope": "LiTo process lifetime only",
        "executable_modified": False,
    }


def invoke_transfer_stone_native_producer(process_handle: int) -> dict[str, Any]:
    """Invoke the recovered use-type-0 producer for the isolated stone once.

    The remote x86 stub first calls the client's own inventory lookup against
    dword_648EBC.  Only when object 0x6C000001 exists does it pass the returned
    native item pointer in ECX to sub_445BD0.  That original routine performs
    its own active-state check and serializes C_USE_ITEM through the original
    encrypted socket path.  The stub is volatile and freed after completion.
    """

    if read_process(
        process_handle,
        CLIENT_INVENTORY_LOOKUP,
        len(CLIENT_INVENTORY_LOOKUP_SIGNATURE),
    ) != CLIENT_INVENTORY_LOOKUP_SIGNATURE:
        raise RuntimeError("native inventory lookup signature changed")
    if read_process(
        process_handle,
        CLIENT_DEFAULT_ITEM_USE,
        len(CLIENT_DEFAULT_ITEM_USE_SIGNATURE),
    ) != CLIENT_DEFAULT_ITEM_USE_SIGNATURE:
        raise RuntimeError("native default item-use producer signature changed")

    inventory_snapshot: dict[str, Any] = {
        "root": "0x00000000",
        "count": 0,
        "table": "0x00000000",
        "object_ids": [],
        "target_present": False,
    }
    root_raw = read_process(process_handle, CLIENT_WORLD_SCENE, 4)
    if len(root_raw) == 4:
        root = struct.unpack("<I", root_raw)[0]
        inventory_snapshot["root"] = f"0x{root:08X}"
        if root:
            count_raw = read_process(process_handle, root + 0x2C, 4)
            table_raw = read_process(process_handle, root + 0x58, 4)
            if len(count_raw) == 4:
                count = max(0, min(struct.unpack("<i", count_raw)[0], 256))
                inventory_snapshot["count"] = count
            else:
                count = 0
            if len(table_raw) == 4:
                table = struct.unpack("<I", table_raw)[0]
                inventory_snapshot["table"] = f"0x{table:08X}"
            else:
                table = 0
            if table and count:
                pointer_raw = read_process(process_handle, table, count * 4)
                object_ids: list[str] = []
                for offset in range(0, len(pointer_raw) - 3, 4):
                    item = struct.unpack_from("<I", pointer_raw, offset)[0]
                    if not item:
                        object_ids.append("0x00000000")
                        continue
                    object_raw = read_process(process_handle, item + 4, 4)
                    object_id = (
                        struct.unpack("<I", object_raw)[0]
                        if len(object_raw) == 4
                        else 0
                    )
                    object_ids.append(f"0x{object_id:08X}")
                inventory_snapshot["object_ids"] = object_ids
                inventory_snapshot["target_present"] = (
                    f"0x{TRANSFER_STONE_PROBE_OBJECT_ID:08X}" in object_ids
                )

    # Do not enter the client's inventory lookup from a foreign thread while
    # the character-to-world transition is still constructing its inventory
    # root.  The read-only snapshot above is sufficient to prove absence; the
    # original lookup and item-use producer are invoked only after the exact
    # socket-delivered object is present.
    if not inventory_snapshot["target_present"]:
        return {
            "status": "waiting_for_inventory_object",
            "object_id": f"0x{TRANSFER_STONE_PROBE_OBJECT_ID:08X}",
            "inventory_lookup": f"0x{CLIENT_INVENTORY_LOOKUP:08X}",
            "producer": f"0x{CLIENT_DEFAULT_ITEM_USE:08X}",
            "inventory_snapshot": inventory_snapshot,
            "thread_created": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }

    # push object; mov ecx,[world/inventory root]; call sub_444900; if found,
    # mov ecx,eax; call sub_445BD0; return 1.  Otherwise return 0.
    stub = bytearray()
    stub += b"\x68" + struct.pack("<I", TRANSFER_STONE_PROBE_OBJECT_ID)
    stub += b"\x8B\x0D" + struct.pack("<I", CLIENT_WORLD_SCENE)
    stub += b"\xB8" + struct.pack("<I", CLIENT_INVENTORY_LOOKUP)
    stub += b"\xFF\xD0\x85\xC0\x74\x11\x8B\xC8"
    stub += b"\xB8" + struct.pack("<I", CLIENT_DEFAULT_ITEM_USE)
    stub += b"\xFF\xD0\xB8\x01\x00\x00\x00\xC2\x04\x00"
    stub += b"\x33\xC0\xC2\x04\x00"

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate native transfer-stone producer page")
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, bytes(stub)) != len(stub):
            raise RuntimeError("could not write native transfer-stone producer stub")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(stub)):
            raise win_error("FlushInstructionCache(transfer-stone producer)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle,
                None,
                0,
                remote,
                None,
                0,
                ctypes.byref(thread_id),
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(transfer-stone producer)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait == WAIT_TIMEOUT:
            return {
                "status": "native_producer_thread_timeout",
                "object_id": f"0x{TRANSFER_STONE_PROBE_OBJECT_ID:08X}",
                "volatile_memory_only": True,
            }
        if wait != WAIT_OBJECT_0:
            raise win_error("WaitForSingleObject(transfer-stone producer)")
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(transfer-stone producer)")
        return {
            "status": (
                "native_producer_invoked"
                if exit_code.value == 1
                else "waiting_for_inventory_object"
            ),
            "object_id": f"0x{TRANSFER_STONE_PROBE_OBJECT_ID:08X}",
            "thread_exit_code": int(exit_code.value),
            "inventory_lookup": f"0x{CLIENT_INVENTORY_LOOKUP:08X}",
            "producer": f"0x{CLIENT_DEFAULT_ITEM_USE:08X}",
            "inventory_snapshot": inventory_snapshot,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def invoke_transfer_stone_inventory_receiver(process_handle: int) -> dict[str, Any]:
    """Give the exact one-item S_7C fixture to LiTo's original receiver.

    The automatic standalone room bridge can stop its main dispatch thread at
    RtlExitUserProcess before later socket frames are dispatched.  This bounded
    diagnostic calls sub_4B2650 directly with the same exact packet body the
    isolated server sent.  It proves native parsing only; normal socket
    dispatcher reachability and phase-4 UI click reachability are not claimed.
    """

    observed = read_process(
        process_handle,
        CLIENT_INVENTORY_BATCH_RECEIVER,
        len(CLIENT_INVENTORY_BATCH_RECEIVER_SIGNATURE),
    )
    if observed != CLIENT_INVENTORY_BATCH_RECEIVER_SIGNATURE:
        raise RuntimeError(
            "native inventory-batch receiver signature changed: "
            + observed.hex(" ").upper()
        )

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate native inventory receiver page")
    packet_address = remote + 0x100
    stub = bytearray()
    stub += b"\xB9" + struct.pack("<I", packet_address)
    stub += b"\xB8" + struct.pack("<I", CLIENT_INVENTORY_BATCH_RECEIVER)
    stub += b"\xFF\xD0\xB8\x01\x00\x00\x00\xC2\x04\x00"
    payload = bytes(stub)
    payload += b"\x00" * (0x100 - len(payload))
    payload += TRANSFER_STONE_PROBE_INVENTORY_PACKET
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, payload) != len(payload):
            raise RuntimeError("could not write native inventory receiver fixture")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(payload)):
            raise win_error("FlushInstructionCache(inventory receiver)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle,
                None,
                0,
                remote,
                None,
                0,
                ctypes.byref(thread_id),
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(inventory receiver)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait != WAIT_OBJECT_0:
            return {
                "status": "native_inventory_receiver_thread_timeout",
                "wait_result": wait,
                "volatile_memory_only": True,
            }
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(inventory receiver)")
        return {
            "status": "native_inventory_receiver_invoked",
            "receiver": f"0x{CLIENT_INVENTORY_BATCH_RECEIVER:08X}",
            "thread_exit_code": int(exit_code.value),
            "plain": TRANSFER_STONE_PROBE_INVENTORY_PACKET.hex(" ").upper(),
            "wire": "S_7C count=1 plus exact transfer-stone S_16 record body",
            "socket_dispatcher_claimed": False,
            "phase_4_ui_claimed": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def snapshot_native_server_directory(process_handle: int) -> dict[str, Any]:
    """Read the two native S_FB vectors without mutating the client."""

    root_raw = read_process(process_handle, CLIENT_SERVER_DIRECTORY_ROOT, 4)
    if len(root_raw) != 4:
        return {"status": "directory_root_unreadable"}
    root = struct.unpack("<I", root_raw)[0]
    if root == 0:
        return {
            "status": "directory_root_absent",
            "root_global": f"0x{CLIENT_SERVER_DIRECTORY_ROOT:08X}",
        }
    header = read_process(process_handle, root, 0x54)
    if len(header) != 0x54:
        return {"status": "directory_header_unreadable", "root": f"0x{root:08X}"}

    def vector(begin_offset: int, end_offset: int, group: str) -> list[dict[str, Any]]:
        begin = struct.unpack_from("<I", header, begin_offset)[0]
        end = struct.unpack_from("<I", header, end_offset)[0]
        if begin == 0 and end == 0:
            return []
        if end < begin or (end - begin) % 0x118 != 0:
            raise RuntimeError(
                f"native S_FB {group} vector is malformed: "
                f"0x{begin:08X}..0x{end:08X}"
            )
        count = (end - begin) // 0x118
        if count > 0x7F:
            raise RuntimeError(f"native S_FB {group} count is out of range: {count}")
        rows: list[dict[str, Any]] = []
        for index in range(count):
            record_address = begin + index * 0x118
            record = read_process(process_handle, record_address, 0x118)
            if len(record) != 0x118:
                raise RuntimeError(
                    f"could not read native S_FB {group} record {index}"
                )
            name_wire = record[:0x101].split(b"\x00", 1)[0]
            row: dict[str, Any] = {
                "index": index,
                "record": f"0x{record_address:08X}",
                "name": name_wire.decode("cp950", errors="replace"),
                "ipv4": ".".join(str(value) for value in record[0x104:0x108]),
                "ipv4_wire": f"0x{struct.unpack_from('<I', record, 0x104)[0]:08X}",
                "port": struct.unpack_from("<H", record, 0x108)[0],
            }
            if group == "rooms":
                row["room_id"] = struct.unpack_from("<I", record, 0x10C)[0]
                row["users"] = struct.unpack_from("<I", record, 0x110)[0]
            else:
                row["users"] = struct.unpack_from("<I", record, 0x110)[0]
                row["capacity"] = struct.unpack_from("<I", record, 0x114)[0]
            rows.append(row)
        return rows

    rooms = vector(0x10, 0x14, "rooms")
    servers = vector(0x20, 0x24, "servers")
    return {
        "status": "native_directory_snapshot",
        "root": f"0x{root:08X}",
        "room_count": len(rooms),
        "server_count": len(servers),
        "rooms": rooms,
        "servers": servers,
    }


def invoke_transfer_stone_directory_receiver(process_handle: int) -> dict[str, Any]:
    """Feed the current 32100 S_FB fixture to the original LiTo receiver."""

    observed = read_process(
        process_handle,
        CLIENT_SERVER_DIRECTORY_RECEIVER,
        len(CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE),
    )
    if observed != CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE:
        raise RuntimeError(
            "native S_FB receiver signature changed: "
            + observed.hex(" ").upper()
        )
    before = snapshot_native_server_directory(process_handle)
    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate native S_FB receiver page")
    packet_address = remote + 0x100
    stub = bytearray()
    stub += b"\xB9" + struct.pack("<I", packet_address)
    stub += b"\xB8" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_RECEIVER)
    stub += b"\xFF\xD0\xB8\x01\x00\x00\x00\xC2\x04\x00"
    page = bytes(stub).ljust(0x100, b"\x90") + TRANSFER_STONE_PROBE_DIRECTORY_PACKET
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, page) != len(page):
            raise RuntimeError("could not write native S_FB receiver fixture")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(page)):
            raise win_error("FlushInstructionCache(S_FB receiver)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle, None, 0, remote, None, 0, ctypes.byref(thread_id)
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(S_FB receiver)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait != WAIT_OBJECT_0:
            return {
                "status": "native_directory_receiver_thread_timeout",
                "wait_result": wait,
                "before": before,
                "volatile_memory_only": True,
            }
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(S_FB receiver)")
        after = snapshot_native_server_directory(process_handle)
        return {
            "status": "native_directory_receiver_invoked",
            "receiver": f"0x{CLIENT_SERVER_DIRECTORY_RECEIVER:08X}",
            "thread_exit_code": int(exit_code.value),
            "plain": TRANSFER_STONE_PROBE_DIRECTORY_PACKET.hex(" ").upper(),
            "before": before,
            "after": after,
            "catalogue_scope": "current authoritative 127.0.0.2:32100 fixture",
            "historical_catalogue_claimed": False,
            "socket_dispatcher_claimed": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def invoke_transfer_stone_server_click(process_handle: int) -> dict[str, Any]:
    """Select the first recovered S_FB group-2 row through sub_475F40."""

    observed = read_process(
        process_handle,
        CLIENT_SERVER_DIRECTORY_CLICK,
        len(CLIENT_SERVER_DIRECTORY_CLICK_SIGNATURE),
    )
    if observed != CLIENT_SERVER_DIRECTORY_CLICK_SIGNATURE:
        raise RuntimeError(
            "native S_FB group-2 click signature changed: "
            + observed.hex(" ").upper()
        )
    snapshot = snapshot_native_server_directory(process_handle)
    servers = snapshot.get("servers", [])
    if snapshot.get("status") != "native_directory_snapshot" or len(servers) != 1:
        return {
            "status": "native_server_click_not_ready",
            "directory_snapshot": snapshot,
        }
    root = int(snapshot["root"], 16)
    root_header = read_process(process_handle, root, 0x54)
    actor = struct.unpack_from("<I", root_header, 0x08)[0]
    if actor == 0:
        return {
            "status": "native_server_click_actor_absent",
            "directory_snapshot": snapshot,
        }
    actor_header = read_process(process_handle, actor, 0x1C)
    if len(actor_header) != 0x1C:
        return {
            "status": "native_server_click_actor_unreadable",
            "directory_snapshot": snapshot,
        }
    x = (struct.unpack_from("<i", actor_header, 0x14)[0]
         + struct.unpack_from("<i", root_header, 0x3C)[0] + 1)
    y = (struct.unpack_from("<i", actor_header, 0x18)[0]
         + struct.unpack_from("<i", root_header, 0x40)[0] + 1)

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate native S_FB click page")
    stub = bytearray()
    for message in (0x200, 0x202):
        stub += b"\x68" + struct.pack("<i", y)
        stub += b"\x68" + struct.pack("<i", x)
        stub += b"\x6A\x00"
        stub += b"\x68" + struct.pack("<I", message)
        stub += b"\xB9" + struct.pack("<I", root)
        stub += b"\xB8" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_CLICK)
        stub += b"\xFF\xD0"
    stub += b"\xB8\x01\x00\x00\x00\xC2\x04\x00"
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, bytes(stub)) != len(stub):
            raise RuntimeError("could not write native S_FB click stub")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(stub)):
            raise win_error("FlushInstructionCache(S_FB click)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle, None, 0, remote, None, 0, ctypes.byref(thread_id)
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(S_FB click)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait != WAIT_OBJECT_0:
            return {
                "status": "native_server_click_thread_timeout",
                "wait_result": wait,
                "directory_snapshot": snapshot,
                "volatile_memory_only": True,
            }
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(S_FB click)")
        return {
            "status": "native_server_click_invoked",
            "handler": f"0x{CLIENT_SERVER_DIRECTORY_CLICK:08X}",
            "messages": ["WM_LBUTTONDOWN 0x0200", "WM_LBUTTONUP 0x0202"],
            "coordinates": {"x": x, "y": y},
            "selected": servers[0],
            "thread_exit_code": int(exit_code.value),
            "native_branch_contract": (
                "same endpoint: sub_46EB30 -> C_D9/C_CB/C_CA(0); "
                "different endpoint: sub_46ED50 -> C_D9 then reconnect"
            ),
            "runtime_branch": "must be classified from wire/connect capture",
            "user_click_claimed": False,
            "historical_catalogue_claimed": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def invoke_rental_town_room_click(process_handle: int) -> dict[str, Any]:
    """Select the sole live S_FB group-1 row through native sub_475DA0.

    The function invokes the same native down/up handler as a mouse click, but
    records that this is a diagnostic call rather than claiming human input.
    It consumes only a directory received by the client's normal socket path.
    """

    snapshot = snapshot_native_server_directory(process_handle)
    rooms = snapshot.get("rooms", [])
    if snapshot.get("status") != "native_directory_snapshot" or len(rooms) != 1:
        return {
            "status": "native_room_click_not_ready",
            "directory_snapshot": snapshot,
        }
    observed = read_process(
        process_handle,
        CLIENT_ROOM_DIRECTORY_CLICK,
        len(CLIENT_ROOM_DIRECTORY_CLICK_SIGNATURE),
    )
    if observed != CLIENT_ROOM_DIRECTORY_CLICK_SIGNATURE:
        raise RuntimeError(
            "native S_FB group-1 click signature changed after directory arrival: "
            + observed.hex(" ").upper()
        )
    root = int(snapshot["root"], 16)
    root_header = read_process(process_handle, root, 0x54)
    actor = struct.unpack_from("<I", root_header, 0x08)[0]
    if actor == 0:
        return {
            "status": "native_room_click_actor_absent",
            "directory_snapshot": snapshot,
        }
    actor_header = read_process(process_handle, actor, 0x1C)
    if len(actor_header) != 0x1C:
        return {
            "status": "native_room_click_actor_unreadable",
            "directory_snapshot": snapshot,
        }
    x = (
        struct.unpack_from("<i", actor_header, 0x14)[0]
        + struct.unpack_from("<i", root_header, 0x34)[0]
        + 1
    )
    y = (
        struct.unpack_from("<i", actor_header, 0x18)[0]
        + struct.unpack_from("<i", root_header, 0x38)[0]
        + 1
    )

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate native S_FB group-1 click page")
    stub = bytearray()
    for message in (0x200, 0x202):
        stub += b"\x68" + struct.pack("<i", y)
        stub += b"\x68" + struct.pack("<i", x)
        stub += b"\x6A\x00"
        stub += b"\x68" + struct.pack("<I", message)
        stub += b"\xB9" + struct.pack("<I", root)
        stub += b"\xB8" + struct.pack("<I", CLIENT_ROOM_DIRECTORY_CLICK)
        stub += b"\xFF\xD0"
    stub += b"\xB8\x01\x00\x00\x00\xC2\x04\x00"
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, bytes(stub)) != len(stub):
            raise RuntimeError("could not write native S_FB group-1 click stub")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(stub)):
            raise win_error("FlushInstructionCache(S_FB group-1 click)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle, None, 0, remote, None, 0, ctypes.byref(thread_id)
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(S_FB group-1 click)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait != WAIT_OBJECT_0:
            return {
                "status": "native_room_click_thread_timeout",
                "wait_result": wait,
                "directory_snapshot": snapshot,
                "volatile_memory_only": True,
            }
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(S_FB group-1 click)")
        return {
            "status": "native_room_click_invoked",
            "handler": f"0x{CLIENT_ROOM_DIRECTORY_CLICK:08X}",
            "messages": ["WM_LBUTTONDOWN 0x0200", "WM_LBUTTONUP 0x0202"],
            "coordinates": {"x": x, "y": y},
            "selected": rooms[0],
            "thread_exit_code": int(exit_code.value),
            "native_branch_contract": (
                "sub_475DA0 -> cache room id -> sub_46F240 -> C_D9; "
                "S_F0/S_F5 then produces native C_CF"
            ),
            "socket_directory_received_naturally": True,
            "user_click_claimed": False,
            "historical_catalogue_claimed": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def invoke_transfer_stone_world_entry_producer(
    process_handle: int,
    action: str,
) -> dict[str, Any]:
    """Use LiTo's original packet writer to reach an isolated probe world.

    This is a loopback-only diagnostic prerequisite for the transfer-stone
    producer.  It does not claim that the rental service used this room path:
    the dedicated fixture enters the ordinary in-memory channel, joins its
    deterministic room 1 with the native C_CF cds envelope, then sends the
    native C_D2 cc ready envelope.  Both calls stay inside LiTo's own encrypted
    socket writer and the volatile page is freed after the remote thread exits.
    """

    if read_process(
        process_handle,
        CLIENT_PACKET_WRITER,
        len(CLIENT_PACKET_WRITER_SIGNATURE),
    ) != CLIENT_PACKET_WRITER_SIGNATURE:
        raise RuntimeError("native packet-writer signature changed")
    if action not in ("join", "ready"):
        raise ValueError(f"unsupported transfer-stone world-entry action: {action}")

    stub = bytearray()
    empty_string_offset = 0x100
    if action == "join":
        # sub_4C6AE0(off_502CA8, C_CF, room 1, "") where off_502CA8 is
        # the selected image's protected `cds` format descriptor.
        stub += b"\x68" + struct.pack("<I", 0)  # fixed after allocation
        stub += b"\x68" + struct.pack("<I", TRANSFER_STONE_PROBE_ROOM_ID)
        stub += b"\x68" + struct.pack("<I", 0xCF)
        stub += b"\x68" + struct.pack("<I", CLIENT_FORMAT_CDS_POINTER)
        stack_bytes = 16
        opcode = 0xCF
        wire = "c d(room=1) s(empty)"
    else:
        # sub_4C6AE0(aCc, C_D2, 1): ready in the ordinary probe room.
        stub += b"\x68" + struct.pack("<I", 1)
        stub += b"\x68" + struct.pack("<I", 0xD2)
        stub += b"\x68" + struct.pack("<I", CLIENT_FORMAT_CC)
        stack_bytes = 12
        opcode = 0xD2
        wire = "c c(ready=1)"
    stub += b"\xB8" + struct.pack("<I", CLIENT_PACKET_WRITER)
    stub += b"\xFF\xD0\x81\xC4" + struct.pack("<I", stack_bytes)
    stub += b"\xB8\x01\x00\x00\x00\xC2\x04\x00"

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate transfer-stone world-entry page")
    if action == "join":
        stub[1:5] = struct.pack("<I", remote + empty_string_offset)
    page = bytes(stub).ljust(empty_string_offset, b"\x90") + b"\x00"
    thread = 0
    completed = False
    try:
        if write_process(process_handle, remote, page) != len(page):
            raise RuntimeError("could not write transfer-stone world-entry stub")
        if not kernel32.FlushInstructionCache(process_handle, remote, len(page)):
            raise win_error("FlushInstructionCache(transfer-stone world entry)")
        thread_id = wt.DWORD()
        thread = int(
            kernel32.CreateRemoteThread(
                process_handle,
                None,
                0,
                remote,
                None,
                0,
                ctypes.byref(thread_id),
            )
            or 0
        )
        if not thread:
            raise win_error("CreateRemoteThread(transfer-stone world entry)")
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait == WAIT_TIMEOUT:
            return {
                "status": f"native_{action}_thread_timeout",
                "opcode": f"0x{opcode:02X}",
                "wire": wire,
            }
        if wait != WAIT_OBJECT_0:
            raise win_error("WaitForSingleObject(transfer-stone world entry)")
        completed = True
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(transfer-stone world entry)")
        return {
            "status": f"native_{action}_producer_invoked",
            "opcode": f"0x{opcode:02X}",
            "wire": wire,
            "thread_exit_code": int(exit_code.value),
            "packet_writer": f"0x{CLIENT_PACKET_WRITER:08X}",
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    finally:
        if thread:
            kernel32.CloseHandle(thread)
        if completed:
            kernel32.VirtualFreeEx(process_handle, remote, 0, MEM_RELEASE)


def rearm_credential_bootstrap_for_game_handoff(
    process_handle: int,
    account: bytearray,
    password: bytearray,
    character: bytearray,
) -> dict[str, Any]:
    """Restore launcher-owned plain cells after manual login mutates them.

    The native login form changes its visible edit cells to the protected
    client's internal representation. The VS/game reconnect instead consumes
    the launcher's ordinary NUL-terminated handoff. Re-arm only credentials
    already supplied to this launcher, without reading or logging the mutated
    process strings.
    """

    for address, value, label in (
        (CLIENT_ACCOUNT_CELLS, bytes(account) + b"\x00", "account"),
        (CLIENT_PASSWORD_CELLS, bytes(password) + b"\x00", "password"),
    ):
        if write_process(process_handle, address, value) != len(value):
            raise RuntimeError(f"could not re-arm {label} for game handoff")
    if character:
        value = bytes(character) + b"\x00"
        if write_process(process_handle, CLIENT_CHARACTER_NAME, value) != len(value):
            raise RuntimeError("could not re-arm character for game handoff")
    if write_process(process_handle, CLIENT_AUTO_LOGIN_FLAG, b"\x01") != 1:
        raise RuntimeError("could not re-arm native game auto-login flag")
    return {
        "status": "rearmed_after_second_loopback_connect",
        "account_length_bytes": len(account),
        "password_length_bytes": len(password),
        "character_name_supplied": bool(character),
        "credentials_logged": False,
        "process_strings_read": False,
        "executable_modified": False,
    }


def native_channel_ui_rearm_trigger(
    *,
    manual_native_selection: bool,
    physical_native_login_session: bool,
    manual_native_screen_baseline: int | None,
    screen_value: int,
    server_list_value: int,
    phase_value: int | None,
    mode_value: int | None,
    current_page_value: int,
    directory_present: bool,
    physical_page_builder_returned: bool,
) -> str | None:
    """Return the proved safe point for restoring byte_62C21C.

    The physical LOGIN -> GAME reconstruction changes directly from the
    source-selector root (mode 2) to the character page (mode 4/page 7).
    sub_496EB0 clears byte_62C21C while replacing that root, so waiting for a
    changed mode-2 screen can never succeed. Once the native page builder has
    returned and S_CB has populated its first real character row, the old root
    is gone and the flag may be restored before the player emits C_C9.

    Keep the older changed-mode-2 trigger for the non-physical manual bridge,
    whose native handler constructs the channel selector without the page-7
    reconstruction step.
    """

    if (
        not manual_native_selection
        or screen_value == 0
        or server_list_value == 0
        or phase_value != 0
    ):
        return None
    if (
        physical_native_login_session
        and physical_page_builder_returned
        and mode_value == 4
        and current_page_value == 7
        and directory_present
    ):
        return "physical_native_page_7_character_row_ready"
    if (
        not physical_native_login_session
        and manual_native_screen_baseline is not None
        and screen_value != manual_native_screen_baseline
        and mode_value == 2
    ):
        return "changed_populated_mode_2_screen"
    return None


def native_channel_source_arm_plan(
    observed_source_flags: bytes,
    observed_special_channel_role: bytes,
    page_builder_invocation: int,
    last_armed_page_builder_invocation: int,
    allow_source_b_handoff: bool = False,
) -> str:
    """Choose the fail-closed source-A re-arm action for one page-7 build.

    The physical LOGIN -> GAME path can reconstruct page 7 more than once.
    Each builder invocation clears the source-mode bytes, while S_C8 success
    also clears source A before leaving that page. Key the arm to the native
    builder invocation so a later reconstruction may be repaired exactly once
    without re-arming after S_C8 inside the already-processed invocation.

    /ltcharsvr leaves the statically recovered role tuple 01 00 01.  That tuple
    intentionally makes sub_4A76D0 emit C_CA(0) directly.  Preserve every
    statically valid launch role exactly as observed and fail closed on any
    impossible combination.
    """

    if page_builder_invocation <= last_armed_page_builder_invocation:
        return "already_processed_page_builder_invocation"
    if observed_special_channel_role not in {
        b"\x00\x00\x00",
        b"\x01\x00\x00",
        b"\x00\x01\x00",
        b"\x01\x00\x01",
        b"\x00\x01\x01",
    }:
        return "unexpected_special_channel_role_fail_closed"
    if allow_source_b_handoff and observed_source_flags == b"\x00\x01":
        return "convert_source_b_to_source_a_preserve_role"
    if observed_source_flags not in {b"\x00\x00", b"\x01\x00"}:
        return "unexpected_source_flags_fail_closed"
    if observed_source_flags == b"\x01\x00":
        return "source_a_already_armed_preserve_role"
    return "write_source_a_preserve_role"


def native_character_directory_batch_complete(
    expected_count_raw: bytes,
    parsed_index_raw: bytes,
    pending_count_raw: bytes,
    slot_markers: bytes,
) -> bool:
    """Confirm that every S_CB row announced by S_03 reached page 7.

    sub_4A7450 clears the class/sex marker pair for all four slots when S_03
    arrives.  sub_4A7550 fills one pair per S_CB row and resets its transient
    index/count globals only after the final row and native page refresh.  The
    retained S_03 count plus both facts form a race-free completion boundary.
    """

    if (
        len(expected_count_raw) != 4
        or len(parsed_index_raw) != 4
        or len(pending_count_raw) != 4
        or len(slot_markers) != 8
    ):
        return False
    expected_count = struct.unpack("<I", expected_count_raw)[0]
    parsed_index = struct.unpack("<I", parsed_index_raw)[0]
    pending_count = struct.unpack("<I", pending_count_raw)[0]
    if not 1 <= expected_count <= 4 or parsed_index != 0 or pending_count != 0:
        return False
    for slot in range(4):
        marker_pair = slot_markers[slot * 2 : slot * 2 + 2]
        populated = marker_pair[0] != 0xFF and marker_pair[1] != 0xFF
        if populated != (slot < expected_count):
            return False
    return True


def install_physical_server_directory_bridge(
    process_handle: int,
) -> dict[str, Any] | None:
    """Create the official-launcher S_FB owner before the first directory parse."""

    observed_receiver = read_process(
        process_handle,
        CLIENT_SERVER_DIRECTORY_RECEIVER,
        len(CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE),
    )
    if observed_receiver != CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE:
        return None
    observed_dispatcher = read_process(
        process_handle,
        CLIENT_PACKET_DISPATCHER,
        len(CLIENT_PACKET_DISPATCHER_SIGNATURE),
    )
    if observed_dispatcher != CLIENT_PACKET_DISPATCHER_SIGNATURE:
        return None
    observed_constructor = read_process(
        process_handle,
        CLIENT_SERVER_DIRECTORY_CONSTRUCTOR,
        len(CLIENT_SERVER_DIRECTORY_CONSTRUCTOR_SIGNATURE),
    )
    if observed_constructor != CLIENT_SERVER_DIRECTORY_CONSTRUCTOR_SIGNATURE:
        return None
    observed_ui_flush = read_process(
        process_handle,
        CLIENT_TOURNAMENT_UI_FLUSH,
        len(CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE),
    )
    if observed_ui_flush != CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE:
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate physical S_FB directory bridge")

    call_count_address = remote + 0x200
    root_snapshot_address = remote + 0x204
    root_created_address = remote + 0x208
    room_count_address = remote + 0x20C
    server_count_address = remote + 0x210
    ui_flush_null_guard_count_address = remote + 0x214
    transition_kind_before_address = remote + 0x218
    transition_kind_normalized_count_address = remote + 0x21C
    stub = bytearray()
    original_fixups: list[int] = []

    def jump_original(opcode: bytes) -> None:
        stub.extend(opcode)
        stub.extend(b"\x00\x00\x00\x00")
        original_fixups.append(len(stub) - 4)

    stub += b"\x9C\x60"  # pushfd; pushad
    stub += b"\xFF\x05" + struct.pack("<I", call_count_address)
    # ECX at receiver entry is saved by pushad at [esp+0x18]. Capture only
    # the two public S_FB group counts, never arbitrary packet/string bytes.
    stub += b"\x8B\x74\x24\x18"
    stub += b"\x8B\x46\x01\xA3" + struct.pack("<I", room_count_address)
    stub += b"\x8B\x46\x05\xA3" + struct.pack("<I", server_count_address)
    # Source-A S_C8 leaves transition kind 2, which makes S_F0 reconnect and
    # S_F5 suppress the cached-room C_CF. Normalize only the exact premium
    # group-1 directory contract; every other S_FB preserves native state.
    transition_skip_fixups: list[int] = []

    def jump_transition_skip(opcode: bytes) -> None:
        stub.extend(opcode)
        stub.extend(b"\x00\x00\x00\x00")
        transition_skip_fixups.append(len(stub) - 4)

    stub += b"\x83\x3D" + struct.pack("<I", room_count_address) + b"\x00"
    jump_transition_skip(b"\x0F\x84")
    stub += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_VS_SPECIAL_CHANNEL_MODE_A)
        + b"\x01"
    )
    jump_transition_skip(b"\x0F\x85")
    stub += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_VS_SPECIAL_CHANNEL_MODE_B)
        + b"\x00"
    )
    jump_transition_skip(b"\x0F\x85")
    stub += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_CHARACTER_SERVER_MODE)
        + b"\x00"
    )
    jump_transition_skip(b"\x0F\x85")
    stub += b"\xA1" + struct.pack("<I", CLIENT_VS_TRANSITION_KIND)
    stub += b"\xA3" + struct.pack("<I", transition_kind_before_address)
    stub += b"\x83\xF8\x02"
    jump_transition_skip(b"\x0F\x85")
    stub += (
        b"\xC7\x05"
        + struct.pack("<I", CLIENT_VS_TRANSITION_KIND)
        + b"\x00\x00\x00\x00"
    )
    stub += b"\xFF\x05" + struct.pack(
        "<I", transition_kind_normalized_count_address
    )
    transition_skip_offset = len(stub)
    for displacement_index in transition_skip_fixups:
        struct.pack_into(
            "<i",
            stub,
            displacement_index,
            transition_skip_offset - (displacement_index + 4),
        )
    stub += b"\xA1" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += b"\x85\xC0"
    have_root_fixup = len(stub) + 2
    stub += b"\x0F\x85\x00\x00\x00\x00"
    stub += b"\x6A\x54"
    allocator_call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        allocator_call_offset + 1,
        CLIENT_NATIVE_ALLOCATOR - (remote + allocator_call_offset + 5),
    )
    stub += b"\x83\xC4\x04\x85\xC0"
    jump_original(b"\x0F\x84")
    stub += b"\x8B\xC8"
    constructor_call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        constructor_call_offset + 1,
        CLIENT_SERVER_DIRECTORY_CONSTRUCTOR
        - (remote + constructor_call_offset + 5),
    )
    stub += b"\x85\xC0"
    jump_original(b"\x0F\x84")
    stub += b"\xA3" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += (
        b"\xC7\x05"
        + struct.pack("<I", root_created_address)
        + struct.pack("<I", 1)
    )
    have_root_offset = len(stub)
    struct.pack_into(
        "<i",
        stub,
        have_root_fixup,
        have_root_offset - (have_root_fixup + 4),
    )
    stub += b"\xA1" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += b"\xA3" + struct.pack("<I", root_snapshot_address)
    original_offset = len(stub)
    for displacement_index in original_fixups:
        struct.pack_into(
            "<i",
            stub,
            displacement_index,
            original_offset - (displacement_index + 4),
        )
    stub += b"\x61\x9D"  # popad; popfd
    stub += CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE
    stub += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_SERVER_DIRECTORY_RECEIVER
         + len(CLIENT_SERVER_DIRECTORY_RECEIVER_SIGNATURE))
        - (remote + len(stub) + 5),
    )

    # LiTo's alternate (mode != 4 or phase < 3) opcode table deliberately
    # drops 0xFB. /ltpremiumsvr's accepted item-set S_C9 enters mode 3/phase 1
    # and immediately asks C_CE, so route only that launch role's socket-owned
    # S_FB through the same native receiver. All other packets and roles resume
    # the byte-for-byte original dispatcher.
    dispatcher_bridge = remote + 0x300
    dispatch = bytearray()
    dispatcher_original_fixups: list[int] = []

    def jump_dispatcher_original(opcode: bytes) -> None:
        dispatch.extend(opcode)
        dispatch.extend(b"\x00\x00\x00\x00")
        dispatcher_original_fixups.append(len(dispatch) - 4)

    dispatch += b"\x80\x39\xFB"  # cmp byte ptr [ecx],0FBh
    jump_dispatcher_original(b"\x0F\x85")
    dispatch += b"\x83\x3D" + struct.pack("<I", 0x0064BBD4) + b"\x04"
    not_primary_fixup = len(dispatch) + 2
    dispatch += b"\x0F\x85\x00\x00\x00\x00"
    dispatch += b"\x83\x3D" + struct.pack("<I", CLIENT_PHASE) + b"\x03"
    jump_dispatcher_original(b"\x0F\x8D")
    not_primary_offset = len(dispatch)
    struct.pack_into(
        "<i",
        dispatch,
        not_primary_fixup,
        not_primary_offset - (not_primary_fixup + 4),
    )
    dispatch += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_VS_SPECIAL_CHANNEL_MODE_A)
        + b"\x01"
    )
    jump_dispatcher_original(b"\x0F\x85")
    dispatch += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_VS_SPECIAL_CHANNEL_MODE_B)
        + b"\x00"
    )
    jump_dispatcher_original(b"\x0F\x85")
    dispatch += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_CHARACTER_SERVER_MODE)
        + b"\x00"
    )
    jump_dispatcher_original(b"\x0F\x85")
    receiver_call_offset = len(dispatch)
    dispatch += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        dispatch,
        receiver_call_offset + 1,
        CLIENT_SERVER_DIRECTORY_RECEIVER
        - (dispatcher_bridge + receiver_call_offset + 5),
    )
    dispatch += b"\xC3"
    dispatcher_original_offset = len(dispatch)
    for displacement_index in dispatcher_original_fixups:
        struct.pack_into(
            "<i",
            dispatch,
            displacement_index,
            dispatcher_original_offset - (displacement_index + 4),
        )
    dispatch += CLIENT_PACKET_DISPATCHER_SIGNATURE
    dispatch += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_PACKET_DISPATCHER + len(CLIENT_PACKET_DISPATCHER_SIGNATURE))
        - (dispatcher_bridge + len(dispatch) + 5),
    )

    # Preserve sub_417970 byte-for-byte whenever its official UI manager
    # exists. In the standalone physical handoff only, its manager can be
    # absent; execute the function's native optional-owner cleanup and final
    # state reset, but omit the impossible null virtual call at 0x0041798C.
    ui_flush_guard = remote + 0x400
    ui_guard = bytearray()
    ui_guard += (
        b"\x83\x3D"
        + struct.pack("<I", CLIENT_TOURNAMENT_UI_MANAGER)
        + b"\x00"
    )
    have_ui_manager_fixup = len(ui_guard) + 2
    ui_guard += b"\x0F\x85\x00\x00\x00\x00"
    ui_guard += b"\xFF\x05" + struct.pack(
        "<I", ui_flush_null_guard_count_address
    )
    ui_guard += b"\x8B\x0D" + struct.pack("<I", CLIENT_ACTIVE_IMAGE_OWNER)
    ui_guard += (
        b"\xC6\x05"
        + struct.pack("<I", CLIENT_IMAGE_CAPTURE_FLAG)
        + b"\x00"
    )
    ui_guard += b"\x85\xC9"
    no_active_owner_fixup = len(ui_guard) + 1
    ui_guard += b"\x74\x00"
    ui_guard += b"\x8B\x01\xFF\x50\x24"
    no_active_owner_offset = len(ui_guard)
    ui_guard[no_active_owner_fixup] = (
        no_active_owner_offset - (no_active_owner_fixup + 1)
    )
    ui_guard += (
        b"\xC7\x05"
        + struct.pack("<I", CLIENT_ACTIVE_IMAGE_OWNER)
        + b"\x00\x00\x00\x00"
        + b"\xC3"
    )
    have_ui_manager_offset = len(ui_guard)
    struct.pack_into(
        "<i",
        ui_guard,
        have_ui_manager_fixup,
        have_ui_manager_offset - (have_ui_manager_fixup + 4),
    )
    ui_guard += CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE
    ui_guard += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_TOURNAMENT_UI_FLUSH
         + len(CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE))
        - (ui_flush_guard + len(ui_guard) + 5),
    )

    page = bytearray(b"\x90" * 0x600)
    page[: len(stub)] = stub
    page[0x200:0x220] = b"\x00" * 0x20
    page[0x300:0x300 + len(dispatch)] = dispatch
    page[0x400:0x400 + len(ui_guard)] = ui_guard
    if write_process(process_handle, remote, bytes(page)) != len(page):
        raise RuntimeError("short write while installing physical S_FB bridge")
    entry_patch = b"\xE9" + struct.pack(
        "<i", remote - (CLIENT_SERVER_DIRECTORY_RECEIVER + 5)
    )
    dispatcher_entry_patch = b"\xE9" + struct.pack(
        "<i", dispatcher_bridge - (CLIENT_PACKET_DISPATCHER + 5)
    )
    ui_flush_entry_patch = (
        b"\xE9"
        + struct.pack(
            "<i", ui_flush_guard - (CLIENT_TOURNAMENT_UI_FLUSH + 5)
        )
        + b"\x90"
    )
    write_process_protected(
        process_handle, CLIENT_SERVER_DIRECTORY_RECEIVER, entry_patch
    )
    write_process_protected(
        process_handle, CLIENT_PACKET_DISPATCHER, dispatcher_entry_patch
    )
    write_process_protected(
        process_handle, CLIENT_TOURNAMENT_UI_FLUSH, ui_flush_entry_patch
    )
    if not kernel32.FlushInstructionCache(process_handle, remote, len(page)):
        raise win_error("FlushInstructionCache(physical S_FB bridge)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_SERVER_DIRECTORY_RECEIVER, len(entry_patch)
    ):
        raise win_error("FlushInstructionCache(physical S_FB entry)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_PACKET_DISPATCHER, len(dispatcher_entry_patch)
    ):
        raise win_error("FlushInstructionCache(physical S_FB dispatcher)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_TOURNAMENT_UI_FLUSH, len(ui_flush_entry_patch)
    ):
        raise win_error("FlushInstructionCache(Tournament UI flush guard)")
    return {
        "status": "installed",
        "receiver": f"0x{CLIENT_SERVER_DIRECTORY_RECEIVER:08X}",
        "receiver_entry_observed": observed_receiver.hex(" ").upper(),
        "entry_patch": entry_patch.hex(" ").upper(),
        "dispatcher": f"0x{CLIENT_PACKET_DISPATCHER:08X}",
        "dispatcher_entry_observed": observed_dispatcher.hex(" ").upper(),
        "dispatcher_entry_patch": dispatcher_entry_patch.hex(" ").upper(),
        "dispatcher_bridge": f"0x{dispatcher_bridge:08X}",
        "alternate_dispatch_scope": (
            "socket S_FB only when /ltpremiumsvr role is 01 00 00"
        ),
        "bridge": f"0x{remote:08X}",
        "call_count_address": f"0x{call_count_address:08X}",
        "call_count": 0,
        "root_snapshot_address": f"0x{root_snapshot_address:08X}",
        "root_snapshot": "0x00000000",
        "root_created_address": f"0x{root_created_address:08X}",
        "root_created": 0,
        "room_count_address": f"0x{room_count_address:08X}",
        "room_count": 0,
        "server_count_address": f"0x{server_count_address:08X}",
        "server_count": 0,
        "ui_flush_guard": f"0x{ui_flush_guard:08X}",
        "ui_flush_entry_observed": observed_ui_flush.hex(" ").upper(),
        "ui_flush_entry_patch": ui_flush_entry_patch.hex(" ").upper(),
        "ui_flush_null_guard_count_address": (
            f"0x{ui_flush_null_guard_count_address:08X}"
        ),
        "ui_flush_null_guard_count": 0,
        "ui_flush_policy": (
            "preserve original sub_417970 when dword_64B8F8 exists; when the "
            "standalone physical handoff has no official UI manager, preserve "
            "its optional active-image cleanup and reset while skipping only "
            "the null virtual call"
        ),
        "transition_kind_address": f"0x{CLIENT_VS_TRANSITION_KIND:08X}",
        "transition_kind_before_address": (
            f"0x{transition_kind_before_address:08X}"
        ),
        "transition_kind_before": 0,
        "transition_kind_normalized_count_address": (
            f"0x{transition_kind_normalized_count_address:08X}"
        ),
        "transition_kind_normalized_count": 0,
        "transition_kind_policy": (
            "convert native source-A value 2 to direct-room value 0 only on "
            "a /ltpremiumsvr S_FB with at least one group-1 row, so S_F0/S_F5 "
            "retain the cached room id and emit C_CF"
        ),
        "constructor": f"0x{CLIENT_SERVER_DIRECTORY_CONSTRUCTOR:08X}",
        "constructor_signature": observed_constructor.hex(" ").upper(),
        "allocation_size": 0x54,
        "credentials_logged": False,
        "volatile_memory_only": True,
        "executable_modified": False,
    }


def install_manual_news_ready_bridge(process_handle: int) -> dict[str, Any] | None:
    """Defer the native S_E6 handler until its UI root exists.

    The original immediate branch remains byte-for-byte in the trampoline and
    still owns the C_E2 writer. The bridge only records an early invocation
    and returns to the packet dispatcher; the runtime monitor re-invokes the
    same native handler once dword_75F6E8 is non-null.
    """

    observed = read_process(
        process_handle,
        CLIENT_LITO_NEWS_HANDLER,
        len(CLIENT_LITO_NEWS_HANDLER_ENTRY),
    )
    if observed != CLIENT_LITO_NEWS_HANDLER_ENTRY:
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate manual S_E6 readiness bridge")

    invoke_address = remote + 0x80
    marker_address = remote + 0x100
    bridge = bytearray()
    bridge += b"\x83\x3D" + struct.pack("<I", 0x0075F6E8) + b"\x00"
    bridge += b"\x75\x0B"
    bridge += (
        b"\xC7\x05"
        + struct.pack("<I", marker_address)
        + struct.pack("<I", 1)
    )
    bridge += b"\xC3"
    bridge += CLIENT_LITO_NEWS_HANDLER_ENTRY
    bridge += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_LITO_NEWS_HANDLER + len(CLIENT_LITO_NEWS_HANDLER_ENTRY))
        - (remote + len(bridge) + 5),
    )

    invoke = bytearray()
    invoke += b"\xB8" + struct.pack("<I", CLIENT_LITO_NEWS_HANDLER)
    invoke += b"\xFF\xD0\x33\xC0\xC2\x04\x00"
    page = bytearray(b"\x90" * 0x104)
    page[: len(bridge)] = bridge
    page[0x80 : 0x80 + len(invoke)] = invoke
    page[0x100:0x104] = b"\x00" * 4
    if write_process(process_handle, remote, bytes(page)) != len(page):
        raise RuntimeError("short write while installing manual S_E6 readiness bridge")
    entry_patch = b"\xE9" + struct.pack(
        "<i", remote - (CLIENT_LITO_NEWS_HANDLER + 5)
    )
    write_process_protected(process_handle, CLIENT_LITO_NEWS_HANDLER, entry_patch)
    if not kernel32.FlushInstructionCache(process_handle, remote, len(page)):
        raise win_error("FlushInstructionCache(manual S_E6 readiness bridge)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_LITO_NEWS_HANDLER, len(entry_patch)
    ):
        raise win_error("FlushInstructionCache(manual S_E6 entry)")
    return {
        "status": "installed",
        "handler": f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
        "handler_entry_observed": observed.hex(" ").upper(),
        "entry_patch": entry_patch.hex(" ").upper(),
        "bridge": f"0x{remote:08X}",
        "invoke": f"0x{invoke_address:08X}",
        "pending_marker": f"0x{marker_address:08X}",
        "native_handler_reinvoked": False,
        "direct_packet_writer_used": False,
        "volatile_memory_only": True,
        "executable_modified": False,
    }


def install_physical_character_page_bridge(
    process_handle: int,
    *,
    expected_character_server_mode: int,
    arm_post_directory_source_a: bool,
    allow_source_b_handoff: bool,
) -> dict[str, Any] | None:
    """Build LiTo's original page 7 before S_E6 releases a non-empty S_03.

    A physical standalone login constructs the loading root, but the retained
    S_03 handler assumes the old official launcher has already called
    sub_496EB0 when count is non-zero.  This exact S_E6 prelude invokes that
    original no-argument builder once, on the client's packet thread, then
    resumes the byte-for-byte original S_E6 handler and C_E2 producer.  The
    caller supplies the native launch-role byte that must still be present;
    /ltcharsvr additionally needs its proven post-directory source-A handoff,
    while ordinary and /ltpremiumsvr physical logins preserve their own flags.
    """

    if expected_character_server_mode not in (0, 1):
        raise ValueError("expected character-server mode must be 0 or 1")

    observed_handler = read_process(
        process_handle,
        CLIENT_LITO_NEWS_HANDLER,
        len(CLIENT_LITO_NEWS_HANDLER_ENTRY),
    )
    if observed_handler != CLIENT_LITO_NEWS_HANDLER_ENTRY:
        return None
    observed_builder = read_process(
        process_handle,
        CLIENT_CHARACTER_PAGE_BUILDER,
        len(CLIENT_CHARACTER_PAGE_BUILDER_SIGNATURE),
    )
    if observed_builder != CLIENT_CHARACTER_PAGE_BUILDER_SIGNATURE:
        return None
    observed_network_tail = read_process(
        process_handle,
        CLIENT_CHARACTER_PAGE_NETWORK_TAIL,
        len(CLIENT_CHARACTER_PAGE_NETWORK_TAIL_SIGNATURE),
    )
    if observed_network_tail != CLIENT_CHARACTER_PAGE_NETWORK_TAIL_SIGNATURE:
        return None
    observed_row_auto_confirm = read_process(
        process_handle,
        CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH,
        len(CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH_BYTES),
    )
    if observed_row_auto_confirm != CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH_BYTES:
        return None
    observed_count_handler = read_process(
        process_handle,
        CLIENT_CHARACTER_COUNT_HANDLER,
        len(CLIENT_CHARACTER_COUNT_HANDLER_ENTRY),
    )
    if observed_count_handler != CLIENT_CHARACTER_COUNT_HANDLER_ENTRY:
        return None
    observed_directory_complete_tail = read_process(
        process_handle,
        CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL,
        len(CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL_BYTES),
    )
    if (
        observed_directory_complete_tail
        != CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL_BYTES
    ):
        return None
    observed_confirm_packet_prefix = read_process(
        process_handle,
        CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX,
        len(CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES),
    )
    if (
        arm_post_directory_source_a
        and observed_confirm_packet_prefix
        != CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES
    ):
        return None
    observed_server_list_handler = read_process(
        process_handle,
        CLIENT_SERVER_LIST_HANDLER,
        len(CLIENT_SERVER_LIST_HANDLER_ENTRY),
    )
    if observed_server_list_handler != CLIENT_SERVER_LIST_HANDLER_ENTRY:
        return None
    observed_auto_refresh_branch = read_process(
        process_handle,
        CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH,
        len(CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH_BYTES),
    )
    if (
        observed_auto_refresh_branch
        != CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH_BYTES
    ):
        return None
    observed_directory_constructor = read_process(
        process_handle,
        CLIENT_SERVER_DIRECTORY_CONSTRUCTOR,
        len(CLIENT_SERVER_DIRECTORY_CONSTRUCTOR_SIGNATURE),
    )
    if (
        observed_directory_constructor
        != CLIENT_SERVER_DIRECTORY_CONSTRUCTOR_SIGNATURE
    ):
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate physical character-page bridge")

    marker_address = remote + 0x200
    invocation_count_address = remote + 0x204
    root_snapshot_address = remote + 0x208
    mode_snapshot_address = remote + 0x20C
    page_snapshot_address = remote + 0x210
    base_root_created_address = remote + 0x214
    directory_root_snapshot_address = remote + 0x218
    directory_root_created_address = remote + 0x21C
    expected_character_count_address = remote + 0x220
    count_hook_address = remote + 0x300
    directory_complete_hook_address = remote + 0x340
    confirm_hook_address = remote + 0x390
    stale_server_list_hook_address = remote + 0x3C0
    stub = bytearray()
    skip_fixups: list[int] = []

    def jump_near(opcode: bytes) -> None:
        stub.extend(opcode)
        stub.extend(b"\x00\x00\x00\x00")
        skip_fixups.append(len(stub) - 4)

    stub += b"\x9C\x60"  # pushfd; pushad
    stub += b"\xFF\x05" + struct.pack("<I", invocation_count_address)
    stub += b"\xA1" + struct.pack("<I", 0x0075F6E8)  # mov eax,[root]
    stub += b"\xA3" + struct.pack("<I", root_snapshot_address)
    stub += b"\x0F\xB6\x15" + struct.pack("<I", 0x0064AFA0)
    stub += b"\x89\x15" + struct.pack("<I", mode_snapshot_address)
    stub += b"\x85\xC0"  # test eax,eax
    have_root_fixup = len(stub) + 2
    stub += b"\x0F\x85\x00\x00\x00\x00"  # jnz have_root
    # Reproduce sub_496EB0's exact page-5 factory branch. The official old
    # launcher supplied this root before the redirected GAME bootstrap; a
    # standalone /ltcharsvr handoff does not.
    stub += b"\x68" + struct.pack("<I", 0x136)
    stub += b"\x6A\x00"
    stub += b"\xBA" + struct.pack("<I", 5)
    stub += b"\xB9" + struct.pack("<I", CLIENT_UI_PAGE_REGISTRY)
    factory_call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        factory_call_offset + 1,
        CLIENT_UI_PAGE_FACTORY - (remote + factory_call_offset + 5),
    )
    stub += b"\xA3" + struct.pack("<I", 0x0075F6E8)
    stub += b"\xA3" + struct.pack("<I", root_snapshot_address)
    stub += b"\x85\xC0"
    jump_near(b"\x0F\x84")  # factory failed: preserve original handler
    stub += b"\xC7\x05" + struct.pack("<I", base_root_created_address) + struct.pack("<I", 1)
    have_root_offset = len(stub)
    struct.pack_into(
        "<i",
        stub,
        have_root_fixup,
        have_root_offset - (have_root_fixup + 4),
    )
    stub += b"\x8B\x50\x0C"  # mov edx,[eax+0x0c]
    stub += b"\x89\x15" + struct.pack("<I", page_snapshot_address)
    stub += (
        b"\x80\x3D"
        + struct.pack("<I", CLIENT_CHARACTER_SERVER_MODE)
        + bytes([expected_character_server_mode])
    )
    jump_near(b"\x0F\x85")  # jne original handler
    # The official launcher creates the 0x54-byte S_FB directory receiver
    # object before GAME bootstrap. A standalone physical login leaves its
    # only global null, so sub_4AC7D0 parses and discards every C_CE refresh.
    # Reproduce the original sub_47CF00 allocation/constructor sequence once
    # on this native packet thread before page 7 can transition to the lobby.
    stub += b"\xA1" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += b"\x85\xC0"
    have_directory_fixup = len(stub) + 2
    stub += b"\x0F\x85\x00\x00\x00\x00"
    stub += b"\x6A\x54"
    allocator_call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        allocator_call_offset + 1,
        CLIENT_NATIVE_ALLOCATOR - (remote + allocator_call_offset + 5),
    )
    stub += b"\x83\xC4\x04\x85\xC0"
    jump_near(b"\x0F\x84")
    stub += b"\x8B\xC8"
    directory_constructor_call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        directory_constructor_call_offset + 1,
        CLIENT_SERVER_DIRECTORY_CONSTRUCTOR
        - (remote + directory_constructor_call_offset + 5),
    )
    stub += b"\x85\xC0"
    jump_near(b"\x0F\x84")
    stub += b"\xA3" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += (
        b"\xC7\x05"
        + struct.pack("<I", directory_root_created_address)
        + struct.pack("<I", 1)
    )
    have_directory_offset = len(stub)
    struct.pack_into(
        "<i",
        stub,
        have_directory_fixup,
        have_directory_offset - (have_directory_fixup + 4),
    )
    stub += b"\xA1" + struct.pack("<I", CLIENT_SERVER_DIRECTORY_ROOT)
    stub += b"\xA3" + struct.pack("<I", directory_root_snapshot_address)
    stub += b"\xA1" + struct.pack("<I", 0x0075F6E8)
    stub += b"\x85\xC0"
    jump_near(b"\x0F\x84")
    stub += b"\x83\x3D" + struct.pack("<I", marker_address) + b"\x00"
    jump_near(b"\x0F\x85")  # one-shot
    stub += b"\x83\x78\x0C\x07"  # cmp dword [eax+0x0c],7
    jump_near(b"\x0F\x84")  # page already exists
    stub += b"\xC7\x05" + struct.pack("<I", marker_address) + struct.pack("<I", 1)
    # /ltcharsvr must remain the process launch role, but sub_496EB0 also uses
    # that byte to turn page 7 into the old launcher's ten-second automatic
    # reconnect page.  Clear it only for the duration of the synchronous page
    # constructor, then restore it before the original S_E6 handler resumes.
    # The native source-A result can therefore still take its role-specific
    # C_CA(0) handoff, while the visible selector remains a stable manual page.
    if expected_character_server_mode == 1:
        # Restore the ordinary page-7 Server descriptor that sub_4966A0 made
        # passive for the now-absent official launcher.  Its untouched
        # sub_492FE0 callback opens sub_498F80 and emits C_C8 only after the
        # player chooses a source row.
        stub += (
            b"\xC7\x05"
            + struct.pack("<I", CLIENT_CHARACTER_SERVER_CONTROL_TYPE)
            + struct.pack("<I", CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_TYPE)
        )
        stub += (
            b"\xC7\x05"
            + struct.pack("<I", CLIENT_CHARACTER_SERVER_CONTROL_RESOURCE)
            + struct.pack("<I", CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_RESOURCE)
        )
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_CHARACTER_SERVER_MODE) + b"\x00"
    call_offset = len(stub)
    stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        stub,
        call_offset + 1,
        CLIENT_CHARACTER_PAGE_BUILDER - (remote + call_offset + 5),
    )
    if expected_character_server_mode == 1:
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_CHARACTER_SERVER_MODE) + b"\x01"
    stub += b"\xC7\x05" + struct.pack("<I", marker_address) + struct.pack("<I", 2)
    original_offset = len(stub)
    for displacement_index in skip_fixups:
        struct.pack_into(
            "<i",
            stub,
            displacement_index,
            original_offset - (displacement_index + 4),
        )
    stub += b"\x61\x9D"  # popad; popfd
    stub += CLIENT_LITO_NEWS_HANDLER_ENTRY
    stub += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_LITO_NEWS_HANDLER + len(CLIENT_LITO_NEWS_HANDLER_ENTRY))
        - (remote + len(stub) + 5),
    )

    count_stub = bytearray()
    count_stub += b"\x50"  # push eax
    count_stub += b"\x0F\xB6\x41\x01"  # movzx eax,byte ptr [ecx+1]
    count_stub += b"\xA3" + struct.pack("<I", expected_character_count_address)
    count_stub += b"\x58"  # pop eax
    count_stub += CLIENT_CHARACTER_COUNT_HANDLER_ENTRY
    count_stub += b"\xE9" + struct.pack(
        "<i",
        (
            CLIENT_CHARACTER_COUNT_HANDLER
            + len(CLIENT_CHARACTER_COUNT_HANDLER_ENTRY)
        )
        - (count_hook_address + len(count_stub) + 5),
    )

    directory_complete_stub = bytearray()
    directory_complete_return_fixups: list[int] = []

    def directory_complete_return_near(opcode: bytes) -> None:
        directory_complete_stub.extend(opcode)
        directory_complete_stub.extend(b"\x00\x00\x00\x00")
        directory_complete_return_fixups.append(
            len(directory_complete_stub) - 4
        )

    native_refresh_call_offset = len(directory_complete_stub)
    directory_complete_stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        directory_complete_stub,
        native_refresh_call_offset + 1,
        CLIENT_CHARACTER_DIRECTORY_NATIVE_REFRESH
        - (directory_complete_hook_address + native_refresh_call_offset + 5),
    )
    # Source B is only the LOGIN -> GAME transport marker.  Once the complete
    # GAME-side directory exists it must be neutral, otherwise the old client
    # keeps opening another GAME connection and rebuilding page 7.  Confirm's
    # guarded hook below writes the exact 01 00 tuple immediately before C_C9.
    directory_complete_stub += (
        b"\x66\xC7\x05"
        + struct.pack("<I", CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A)
        + b"\x00\x00"
    )
    # The completed native page owns this global timer slot.  sub_496EB0
    # normally writes the same sub_495E40 value, but the standalone handoff's
    # alternating reconnect callbacks can overwrite it before the final row.
    # Reasserting the original page-7 dispatcher here ends the Loading/list
    # cycle without changing any login, lobby, room, or battle dispatcher.
    directory_complete_stub += (
        b"\xC7\x05"
        + struct.pack("<I", CLIENT_GLOBAL_RENDER_HANDLER)
        + struct.pack("<I", CLIENT_CHARACTER_PAGE_TIMER_CALLBACK)
    )
    directory_complete_stub += b"\x8B\x0D" + struct.pack(
        "<I", CLIENT_UI_ROOT
    )
    directory_complete_stub += b"\x85\xC9"  # test ecx,ecx
    directory_complete_return_near(b"\x0F\x84")
    directory_complete_stub += b"\x83\x79\x0C\x07"
    directory_complete_return_near(b"\x0F\x85")
    directory_complete_stub += b"\xFF\x35" + struct.pack(
        "<I", CLIENT_CHARACTER_SELECTED_INDEX
    )
    child_call_offset = len(directory_complete_stub)
    directory_complete_stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        directory_complete_stub,
        child_call_offset + 1,
        CLIENT_UI_CHILD_BY_INDEX
        - (directory_complete_hook_address + child_call_offset + 5),
    )
    directory_complete_stub += b"\x85\xC0"  # test eax,eax
    directory_complete_return_near(b"\x0F\x84")
    directory_complete_stub += b"\x8B\xC8\x8B\x10\xFF\x52\x38"
    local_refresh_call_offset = len(directory_complete_stub)
    directory_complete_stub += b"\xE8\x00\x00\x00\x00"
    struct.pack_into(
        "<i",
        directory_complete_stub,
        local_refresh_call_offset + 1,
        CLIENT_CHARACTER_DIRECTORY_LOCAL_UI_REFRESH
        - (directory_complete_hook_address + local_refresh_call_offset + 5),
    )
    directory_complete_return_offset = len(directory_complete_stub)
    directory_complete_stub += b"\xC3"
    for displacement_index in directory_complete_return_fixups:
        struct.pack_into(
            "<i",
            directory_complete_stub,
            displacement_index,
            directory_complete_return_offset - (displacement_index + 4),
        )

    confirm_stub = bytearray()
    confirm_stub += b"\x50"  # push eax
    confirm_stub += b"\xA1" + struct.pack("<I", CLIENT_UI_ROOT)
    confirm_stub += b"\x85\xC0"  # test eax,eax
    confirm_skip_fixup = len(confirm_stub) + 2
    confirm_stub += b"\x0F\x84\x00\x00\x00\x00"
    confirm_stub += b"\x83\x78\x0C\x07"
    confirm_skip_page_fixup = len(confirm_stub) + 2
    confirm_stub += b"\x0F\x85\x00\x00\x00\x00"
    confirm_stub += (
        b"\x66\xC7\x05"
        + struct.pack("<I", CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A)
        + b"\x01\x00"
    )
    confirm_skip_offset = len(confirm_stub)
    confirm_stub += b"\x58"  # pop eax
    confirm_stub += CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES
    confirm_stub += b"\xE9" + struct.pack(
        "<i",
        (
            CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX
            + len(CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX_BYTES)
        )
        - (confirm_hook_address + len(confirm_stub) + 5),
    )
    for displacement_index in (confirm_skip_fixup, confirm_skip_page_fixup):
        struct.pack_into(
            "<i",
            confirm_stub,
            displacement_index,
            confirm_skip_offset - (displacement_index + 4),
        )

    stale_server_list_stub = bytearray()
    stale_server_list_stub += b"\x50"  # push eax
    stale_server_list_stub += b"\xA1" + struct.pack("<I", CLIENT_UI_ROOT)
    stale_server_list_stub += b"\x85\xC0"  # test eax,eax
    stale_server_list_original_fixups: list[int] = []
    for opcode in (b"\x0F\x84",):
        stale_server_list_stub += opcode + b"\x00\x00\x00\x00"
        stale_server_list_original_fixups.append(len(stale_server_list_stub) - 4)
    stale_server_list_stub += b"\x83\x78\x0C\x07"
    stale_server_list_stub += b"\x0F\x85\x00\x00\x00\x00"
    stale_server_list_original_fixups.append(len(stale_server_list_stub) - 4)
    stale_server_list_stub += b"\x58\xC3"  # pop eax; ret on stale page-7 list
    stale_server_list_original_offset = len(stale_server_list_stub)
    stale_server_list_stub += b"\x58"  # pop eax
    stale_server_list_stub += CLIENT_SERVER_LIST_HANDLER_ENTRY
    stale_server_list_stub += b"\xE9" + struct.pack(
        "<i",
        (CLIENT_SERVER_LIST_HANDLER + len(CLIENT_SERVER_LIST_HANDLER_ENTRY))
        - (stale_server_list_hook_address + len(stale_server_list_stub) + 5),
    )
    for displacement_index in stale_server_list_original_fixups:
        struct.pack_into(
            "<i",
            stale_server_list_stub,
            displacement_index,
            stale_server_list_original_offset - (displacement_index + 4),
        )

    page = bytearray(b"\x90" * 0x400)
    page[: len(stub)] = stub
    page[0x200:0x230] = b"\x00" * 0x30
    page[0x300 : 0x300 + len(count_stub)] = count_stub
    page[0x340 : 0x340 + len(directory_complete_stub)] = (
        directory_complete_stub
    )
    page[0x390 : 0x390 + len(confirm_stub)] = confirm_stub
    page[0x3C0 : 0x3C0 + len(stale_server_list_stub)] = (
        stale_server_list_stub
    )
    if write_process(process_handle, remote, bytes(page)) != len(page):
        raise RuntimeError("short write while installing physical character-page bridge")
    entry_patch = b"\xE9" + struct.pack(
        "<i", remote - (CLIENT_LITO_NEWS_HANDLER + 5)
    )
    write_process_protected(process_handle, CLIENT_LITO_NEWS_HANDLER, entry_patch)
    write_process_protected(
        process_handle, CLIENT_CHARACTER_PAGE_NETWORK_TAIL, b"\xC3\x90"
    )
    write_process_protected(
        process_handle,
        CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH,
        CLIENT_CHARACTER_ROW_LOCAL_ONLY_BRANCH_BYTES,
    )
    count_entry_patch = b"\xE9" + struct.pack(
        "<i", count_hook_address - (CLIENT_CHARACTER_COUNT_HANDLER + 5)
    ) + b"\x90\x90"
    write_process_protected(
        process_handle,
        CLIENT_CHARACTER_COUNT_HANDLER,
        count_entry_patch,
    )
    directory_complete_entry_patch = b"\xE9" + struct.pack(
        "<i",
        directory_complete_hook_address
        - (CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL + 5),
    )
    write_process_protected(
        process_handle,
        CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL,
        directory_complete_entry_patch,
    )
    confirm_entry_patch = b"\xE9" + struct.pack(
        "<i",
        confirm_hook_address - (CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX + 5),
    )
    if arm_post_directory_source_a:
        write_process_protected(
            process_handle,
            CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX,
            confirm_entry_patch,
        )
    stale_server_list_entry_patch = b"\xE9" + struct.pack(
        "<i",
        stale_server_list_hook_address - (CLIENT_SERVER_LIST_HANDLER + 5),
    ) + b"\x90"
    write_process_protected(
        process_handle,
        CLIENT_SERVER_LIST_HANDLER,
        stale_server_list_entry_patch,
    )
    write_process_protected(
        process_handle,
        CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH,
        CLIENT_CHARACTER_PAGE_STABLE_RETURN_BRANCH_BYTES,
    )
    if not kernel32.FlushInstructionCache(process_handle, remote, len(page)):
        raise win_error("FlushInstructionCache(physical character-page bridge)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_LITO_NEWS_HANDLER, len(entry_patch)
    ):
        raise win_error("FlushInstructionCache(physical character-page entry)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_CHARACTER_PAGE_NETWORK_TAIL, 2
    ):
        raise win_error("FlushInstructionCache(physical page network tail)")
    if not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH,
        len(CLIENT_CHARACTER_ROW_LOCAL_ONLY_BRANCH_BYTES),
    ):
        raise win_error("FlushInstructionCache(character row local selection)")
    if not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_CHARACTER_COUNT_HANDLER,
        len(count_entry_patch),
    ):
        raise win_error("FlushInstructionCache(character count capture)")
    if not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL,
        len(directory_complete_entry_patch),
    ):
        raise win_error("FlushInstructionCache(character directory final refresh)")
    if arm_post_directory_source_a and not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX,
        len(confirm_entry_patch),
    ):
        raise win_error("FlushInstructionCache(character confirm source arm)")
    if not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_SERVER_LIST_HANDLER,
        len(stale_server_list_entry_patch),
    ):
        raise win_error("FlushInstructionCache(stale server-list guard)")
    if not kernel32.FlushInstructionCache(
        process_handle,
        CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH,
        len(CLIENT_CHARACTER_PAGE_STABLE_RETURN_BRANCH_BYTES),
    ):
        raise win_error("FlushInstructionCache(character page auto refresh guard)")
    result = {
        "status": "installed",
        "handler": f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
        "handler_entry_observed": observed_handler.hex(" ").upper(),
        "entry_patch": entry_patch.hex(" ").upper(),
        "bridge": f"0x{remote:08X}",
        "marker": f"0x{marker_address:08X}",
        "marker_value": 0,
        "invocation_count_address": f"0x{invocation_count_address:08X}",
        "invocation_count": 0,
        "root_snapshot_address": f"0x{root_snapshot_address:08X}",
        "root_snapshot": "0x00000000",
        "mode_snapshot_address": f"0x{mode_snapshot_address:08X}",
        "mode_snapshot": 0,
        "page_snapshot_address": f"0x{page_snapshot_address:08X}",
        "page_snapshot": 0,
        "base_root_created_address": f"0x{base_root_created_address:08X}",
        "base_root_created": 0,
        "directory_root_snapshot_address": (
            f"0x{directory_root_snapshot_address:08X}"
        ),
        "directory_root_snapshot": "0x00000000",
        "directory_root_created_address": (
            f"0x{directory_root_created_address:08X}"
        ),
        "directory_root_created": 0,
        "expected_character_count_address": (
            f"0x{expected_character_count_address:08X}"
        ),
        "expected_character_count": 0,
        "character_count_handler": f"0x{CLIENT_CHARACTER_COUNT_HANDLER:08X}",
        "character_count_handler_observed": (
            observed_count_handler.hex(" ").upper()
        ),
        "character_count_entry_patch": count_entry_patch.hex(" ").upper(),
        "character_count_hook": f"0x{count_hook_address:08X}",
        "character_directory_final_refresh": {
            "address": (
                f"0x{CLIENT_CHARACTER_DIRECTORY_COMPLETE_TAIL:08X}"
            ),
            "observed": observed_directory_complete_tail.hex(" ").upper(),
            "entry_patch": directory_complete_entry_patch.hex(" ").upper(),
            "hook": f"0x{directory_complete_hook_address:08X}",
            "native_refresh": (
                f"0x{CLIENT_CHARACTER_DIRECTORY_NATIVE_REFRESH:08X}"
            ),
            "local_ui_refresh": (
                f"0x{CLIENT_CHARACTER_DIRECTORY_LOCAL_UI_REFRESH:08X}"
            ),
            "selected_index_address": (
                f"0x{CLIENT_CHARACTER_SELECTED_INDEX:08X}"
            ),
            "policy": (
                "preserve the original final-row page refresh, reselect the "
                "current native row after all four model resources exist, then "
                "repaint its preview once"
            ),
            "network_packet_emitted": False,
            "completed_source_flags": "00 00",
            "timer_callback_address": (
                f"0x{CLIENT_GLOBAL_RENDER_HANDLER:08X}"
            ),
            "restored_timer_callback": (
                f"0x{CLIENT_CHARACTER_PAGE_TIMER_CALLBACK:08X}"
            ),
            "replaced_reconnect_callbacks": [
                f"0x{CLIENT_LOGIN_RECONNECT_TIMER_CALLBACK:08X}",
                f"0x{CLIENT_GAME_RECONNECT_TIMER_CALLBACK:08X}",
            ],
            "ten_second_reconnect_loop_disabled": True,
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "character_directory_completion_policy": (
            "retain S_03 count and wait for every announced S_CB class/sex "
            "marker plus the native final-row index reset before arming source A"
        ),
        "automatic_source_refresh_suppression": {
            "address": f"0x{CLIENT_CHARACTER_PAGE_AUTO_REFRESH_BRANCH:08X}",
            "observed": observed_auto_refresh_branch.hex(" ").upper(),
            "replacement": (
                CLIENT_CHARACTER_PAGE_STABLE_RETURN_BRANCH_BYTES.hex(" ").upper()
            ),
            "policy": (
                "route the retained old-launcher 64AF9D branch into the stable "
                "page-7 return before it can emit blank C8 00 and Loading"
            ),
            "page_seven_loading_loop_disabled": True,
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "stale_server_list_suppression": {
            "address": f"0x{CLIENT_SERVER_LIST_HANDLER:08X}",
            "observed": observed_server_list_handler.hex(" ").upper(),
            "entry_patch": stale_server_list_entry_patch.hex(" ").upper(),
            "hook": f"0x{stale_server_list_hook_address:08X}",
            "policy": (
                "return from a late S_SERVER_LIST only while the current native "
                "UI root is page 7; preserve the first source selector and every "
                "non-character-page server-list path byte-for-byte"
            ),
            "suppressed_packet_sequence": "C35 then blank C8 00",
            "reconnect_loop_disabled": True,
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "directory_root_global": f"0x{CLIENT_SERVER_DIRECTORY_ROOT:08X}",
        "directory_root_constructor": (
            f"0x{CLIENT_SERVER_DIRECTORY_CONSTRUCTOR:08X}"
        ),
        "directory_root_constructor_signature": (
            observed_directory_constructor.hex(" ").upper()
        ),
        "directory_root_allocation_size": 0x54,
        "base_page_factory": f"0x{CLIENT_UI_PAGE_FACTORY:08X}",
        "base_page_registry": f"0x{CLIENT_UI_PAGE_REGISTRY:08X}",
        "base_page_resource": "0x0136",
        "base_page_index": 5,
        "expected_character_server_mode": expected_character_server_mode,
        "page_builder_role_scope": {
            "character_server_mode_address": (
                f"0x{CLIENT_CHARACTER_SERVER_MODE:08X}"
            ),
            "builder_value": 0,
            "restored_value": expected_character_server_mode,
            "policy": (
                "build a stable ordinary manual selector while preserving the "
                "native /ltcharsvr role before the original S_E6 handler resumes"
                if expected_character_server_mode == 1
                else "preserve the ordinary native selector role"
            ),
            "ten_second_automatic_page_disabled": (
                expected_character_server_mode == 1
            ),
            "volatile_memory_only": True,
        },
        "page_builder": f"0x{CLIENT_CHARACTER_PAGE_BUILDER:08X}",
        "page_builder_signature": observed_builder.hex(" ").upper(),
        "page_resource": "0x1D3A",
        "page_index": 7,
        "character_row_selection_policy": {
            "address": f"0x{CLIENT_CHARACTER_ROW_AUTO_CONFIRM_BRANCH:08X}",
            "observed": observed_row_auto_confirm.hex(" ").upper(),
            "replacement": (
                CLIENT_CHARACTER_ROW_LOCAL_ONLY_BRANCH_BYTES.hex(" ").upper()
            ),
            "row_click": "update native row selection and preview only",
            "confirm_click": "retain native C_C9 producer sub_4928C0",
            "loading_page_on_row_click_disabled": True,
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "character_server_control": {
            "type_address": f"0x{CLIENT_CHARACTER_SERVER_CONTROL_TYPE:08X}",
            "native_type": CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_TYPE,
            "resource_address": (
                f"0x{CLIENT_CHARACTER_SERVER_CONTROL_RESOURCE:08X}"
            ),
            "native_resource": (
                f"0x{CLIENT_CHARACTER_SERVER_CONTROL_NATIVE_RESOURCE:04X}"
            ),
            "native_callback": (
                f"0x{CLIENT_CHARACTER_SERVER_CONTROL_CALLBACK:08X}"
            ),
            "policy": (
                "restore the original interactive Server descriptor before "
                "each standalone page-7 construction"
            ),
            "database_refresh_packet": "native C_C8 after source-row click",
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "late_duplicate_source_request_suppression": {
            "address": f"0x{CLIENT_CHARACTER_PAGE_NETWORK_TAIL:08X}",
            "observed": observed_network_tail.hex(" ").upper(),
            "replacement": "C3 90",
            "scope": "physical standalone GAME page reconstruction only",
            "reason": (
                "the original launcher issued C_C8 before redirect and sub_496EB0 "
                "has already constructed page 7 before this tail; return here to "
                "suppress the duplicate producer without entering loc_496FC6, whose "
                "mode-3 path opens a second retained-source modal"
            ),
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "post_directory_channel_selector_handoff": {
            "status": (
                "armed_only_by_native_character_confirm"
                if arm_post_directory_source_a
                else "not_required_preserve_native_launch_role"
            ),
            "source_a_flag_address": (
                f"0x{CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A:08X}"
            ),
            "source_b_flag_address": (
                f"0x{CLIENT_CHARACTER_AUTO_SELECT_SOURCE_B:08X}"
            ),
            "special_channel_mode_a_address": (
                f"0x{CLIENT_VS_SPECIAL_CHANNEL_MODE_A:08X}"
            ),
            "special_channel_mode_b_address": (
                f"0x{CLIENT_VS_SPECIAL_CHANNEL_MODE_B:08X}"
            ),
            "character_server_mode_address": (
                f"0x{CLIENT_CHARACTER_SERVER_MODE:08X}"
            ),
            "armed_value": 1,
            "allow_source_b_handoff": allow_source_b_handoff,
            "special_channel_role_policy": "preserve_native_launch_role",
            "confirm_packet_prefix": (
                f"0x{CLIENT_CHARACTER_CONFIRM_PACKET_PREFIX:08X}"
            ),
            "confirm_packet_prefix_observed": (
                observed_confirm_packet_prefix.hex(" ").upper()
            ),
            "confirm_hook": (
                f"0x{confirm_hook_address:08X}"
                if arm_post_directory_source_a
                else None
            ),
            "confirm_entry_patch": (
                confirm_entry_patch.hex(" ").upper()
                if arm_post_directory_source_a
                else None
            ),
            "reason": (
                (
                    "keep both source bytes clear while S_CB parses and while any "
                    "retained LOGIN socket can receive S_SERVER_LIST; arm source A "
                    "atomically inside the page-7 Confirm C_C9 producer so S_C9 "
                    "uses the native /ltcharsvr C_CA(0) handoff without creating "
                    "an automatic blank C8 reconnect loop"
                )
                if arm_post_directory_source_a
                else (
                    "ordinary and /ltpremiumsvr physical login already retain the "
                    "native source and special-channel role bytes; only reconstruct "
                    "page 7 and never apply the /ltcharsvr source-A shortcut"
                )
            ),
            "character_name_logged": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        },
        "initial_character_row_activation": {
            "status": "disabled_preserve_physical_character_selection",
            "delay_after_complete_seconds": 0.75,
            "attempt_interval_seconds": 1.0,
            "attempts_per_directory": 0,
            "input_path": "native WM_LBUTTONDOWN/UP on current row",
            "row_click_network_disabled": True,
            "character_name_logged": False,
        },
        "original_s_e6_handler_preserved": True,
        "direct_packet_writer_used": False,
        "volatile_memory_only": True,
        "executable_modified": False,
    }
    return result


def invoke_manual_news_ready_bridge(
    process_handle: int, bridge_report: dict[str, Any]
) -> dict[str, Any]:
    """Re-enter sub_48F500 after its intercepted early S_E6 invocation."""

    marker_address = int(str(bridge_report["pending_marker"]), 16)
    marker_raw = read_process(process_handle, marker_address, 4)
    marker = struct.unpack("<I", marker_raw)[0] if len(marker_raw) == 4 else 0
    if marker != 1:
        return {
            "status": "waiting_for_early_s_e6",
            "pending_marker_value": marker,
        }

    invoke_address = int(str(bridge_report["invoke"]), 16)
    thread_id = wt.DWORD()
    thread = int(
        kernel32.CreateRemoteThread(
            process_handle,
            None,
            0,
            invoke_address,
            None,
            0,
            ctypes.byref(thread_id),
        )
        or 0
    )
    if not thread:
        raise win_error("CreateRemoteThread(manual S_E6 readiness bridge)")
    try:
        wait = int(kernel32.WaitForSingleObject(thread, 5_000))
        if wait != WAIT_OBJECT_0:
            return {
                "status": "native_handler_thread_timeout",
                "wait_result": wait,
                "pending_marker_value": marker,
            }
        exit_code = wt.DWORD()
        if not kernel32.GetExitCodeThread(thread, ctypes.byref(exit_code)):
            raise win_error("GetExitCodeThread(manual S_E6 readiness bridge)")
    finally:
        kernel32.CloseHandle(thread)
    if write_process(process_handle, marker_address, struct.pack("<I", 2)) != 4:
        raise RuntimeError("could not seal manual S_E6 readiness marker")
    return {
        "status": "native_handler_reinvoked",
        "pending_marker_value": 2,
        "thread_id": int(thread_id.value),
        "thread_exit_code": int(exit_code.value),
        "handler": f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
        "direct_packet_writer_used": False,
        "volatile_memory_only": True,
        "executable_modified": False,
    }


def load_server_profile(path: Path, allow_non_loopback: bool) -> dict[str, Any]:
    profile_path = path.resolve()
    payload = json.loads(profile_path.read_text(encoding="utf-8"))
    schema_version = int(payload.get("schemaVersion", 0))
    if schema_version not in (1, 2, 3, 4):
        raise ValueError("server profile schemaVersion must be 1, 2, 3, or 4")
    profile_id = str(payload.get("profileId", "")).strip()
    if not profile_id or any(
        character not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
        for character in profile_id
    ):
        raise ValueError("server profileId may contain only letters, digits, dot, dash, underscore")
    address = ipaddress.IPv4Address(str(payload.get("host", "")).strip())
    port = int(payload.get("port", 0))
    if not 1 <= port <= 65535:
        raise ValueError("server profile port must be between 1 and 65535")
    if not address.is_loopback and not allow_non_loopback:
        raise ValueError(
            "non-loopback server profile requires --allow-non-loopback-redirect"
        )
    config_fields = {
        "local_server_config": str(payload.get("localServerConfig", "")).strip(),
        "rental_source_server_config": str(
            payload.get("rentalSourceServerConfig", "")
        ).strip(),
        "rental_battle_server_config": str(
            payload.get("rentalBattleServerConfig", "")
        ).strip(),
    }
    for field, value in config_fields.items():
        if value and (Path(value).name != value or not value.lower().endswith(".properties")):
            raise ValueError(f"server profile {field} must be a .properties file name")
    if schema_version >= 2 and address.is_loopback and not config_fields["local_server_config"]:
        raise ValueError("loopback server profile requires localServerConfig")
    if schema_version >= 3 and address.is_loopback and (
        bool(config_fields["rental_source_server_config"])
        != bool(config_fields["rental_battle_server_config"])
    ):
        raise ValueError("loopback rental server configs must be supplied together; StandardArena may omit both")
    if schema_version >= 4:
        classic_source_names = payload.get("classicSourceNames", [])
        static_database = str(payload.get("staticDatabase", "")).strip()
        dynamic_databases = payload.get("dynamicDatabases", [])
        if (
            not isinstance(classic_source_names, list)
            or not classic_source_names
            or any(
                not isinstance(name, str)
                or not name.strip()
                or len(name) > 63
                or any(ord(character) < 32 or ord(character) == 127 for character in name)
                for name in classic_source_names
            )
            or len({name.casefold() for name in classic_source_names})
            != len(classic_source_names)
        ):
            raise ValueError(
                "schemaVersion 4 classicSourceNames must be unique non-empty names up to 63 characters"
            )
        database_identifier = lambda value: bool(value) and all(
            character.isascii() and (character.isalnum() or character == "_")
            for character in value
        )
        if (
            not database_identifier(static_database)
            or not isinstance(dynamic_databases, list)
            or len(dynamic_databases) != len(classic_source_names)
            or any(
                not isinstance(database, str) or not database_identifier(database)
                for database in dynamic_databases
            )
            or len({database.casefold() for database in dynamic_databases})
            != len(dynamic_databases)
            or static_database.casefold()
            in {database.casefold() for database in dynamic_databases}
        ):
            raise ValueError(
                "schemaVersion 4 requires one staticDatabase and one unique dynamicDatabase per classic source"
            )
    native_sources=[]
    if 'nativeSourceIds' in payload:
        ids=payload['nativeSourceIds']
        if not isinstance(ids,list) or len(ids)!=len(classic_source_names) or len(ids)>8 or len(set(ids))!=len(ids) or any(type(i)!=int or not 1<=i<=8 for i in ids):
            raise ValueError('Invalid nativeSourceIds')
        base=int(payload['nativeSourcePortBase'])
        if not 1024<=base<=65527: raise ValueError('Invalid nativeSourcePortBase')
        native_sources=[{'id':i,'name':name,'database':database,'port':base+i}
                        for i,name,database in zip(ids,classic_source_names,dynamic_databases)]
    origin_source_id = str(payload.get("originSourceId", profile_id)).strip()
    origin_source_name = str(
        payload.get("originSourceName", payload.get("displayName", profile_id))
    ).strip()
    if not origin_source_id or any(
        character not in "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789._-"
        for character in origin_source_id
    ):
        raise ValueError("originSourceId may contain only letters, digits, dot, dash, underscore")
    if not origin_source_name:
        raise ValueError("originSourceName must not be empty")
    return {
        "schema_version": schema_version,
        "native_sources": native_sources,
        "profile_id": profile_id,
        "display_name": str(payload.get("displayName", profile_id)).strip() or profile_id,
        "origin_source_id": origin_source_id,
        "origin_source_name": origin_source_name,
        **config_fields,
        "host": str(address),
        "port": port,
        "is_loopback": address.is_loopback,
        "source_path": str(profile_path),
        "packed_ip": address.packed,
        "classic_source_names": (
            [str(name).strip() for name in payload.get("classicSourceNames", [])]
            if schema_version >= 4 else []
        ),
        "dynamic_databases": (
            [str(name).strip() for name in payload.get("dynamicDatabases", [])]
            if schema_version >= 4 else []
        ),
    }


def install_connect_redirect(
    process_handle: int,
    pid: int,
    profile: dict[str, Any],
    manual_native_selection: bool = False,
    launcher_credentials: tuple[bytearray, bytearray, bytearray] | None = None,
    waiting_report: dict[str, Any] | None = None,
) -> dict[str, Any] | None:
    readiness: dict[str, Any] = {
        "connect_routine_signature": read_process(
            process_handle, CONNECT_ROUTINE, len(CONNECT_ROUTINE_SIGNATURE)
        ).hex(" ").upper()
    }
    if waiting_report is not None:
        waiting_report["readiness"] = readiness
    pointer_bytes = read_process(process_handle, CONNECT_IAT_SLOT, 4)
    readiness["iat_slot_bytes"] = pointer_bytes.hex(" ").upper()
    if len(pointer_bytes) != 4:
        readiness["wait_reason"] = "iat_slot_unreadable"
        return None
    iat_original_connect = struct.unpack("<I", pointer_bytes)[0]
    readiness["iat_slot_value"] = f"0x{iat_original_connect:08X}"
    observed_thunk = read_process(process_handle, CONNECT_DIRECT_THUNK, 6)
    readiness["direct_thunk_bytes"] = observed_thunk.hex(" ").upper()
    if len(observed_thunk) == 6 and observed_thunk[0] == 0xE9:
        thunk_layout = "jmp_rel32"
        original_connect = (
            CONNECT_DIRECT_THUNK
            + 5
            + struct.unpack("<i", observed_thunk[1:5])[0]
        ) & 0xFFFFFFFF
    elif len(observed_thunk) == 6 and observed_thunk[:2] == b"\x90\xE9":
        thunk_layout = "nop_jmp_rel32"
        original_connect = (
            CONNECT_DIRECT_THUNK
            + 6
            + struct.unpack("<i", observed_thunk[2:6])[0]
        ) & 0xFFFFFFFF
    else:
        readiness["wait_reason"] = "direct_thunk_not_ready"
        return None
    readiness["direct_thunk_layout"] = thunk_layout
    readiness["direct_thunk_target"] = f"0x{original_connect:08X}"
    modules = module_snapshot(pid)
    readiness["socket_modules"] = [
        {
            "name": module["name"],
            "base": module["base"],
            "size": module["size"],
        }
        for module in modules
        if module["name"].casefold() in {"wsock32.dll", "ws2_32.dll"}
    ]
    socket_module = next(
        (
            module
            for module in modules
            if module["name"].casefold() in {"wsock32.dll", "ws2_32.dll"}
            and int(module["base"], 16)
            <= original_connect
            < int(module["base"], 16) + int(module["size"])
        ),
        None,
    )
    if socket_module is None:
        readiness["wait_reason"] = "direct_thunk_target_not_in_socket_module"
        return None
    iat_module = next(
        (
            module
            for module in modules
            if module["name"].casefold() in {"wsock32.dll", "ws2_32.dll"}
            and int(module["base"], 16)
            <= iat_original_connect
            < int(module["base"], 16) + int(module["size"])
        ),
        None,
    )
    if iat_module is None:
        readiness["wait_reason"] = "iat_target_not_in_socket_module"
        return None
    readiness["wait_reason"] = "ready"

    source_session=0
    if profile.get('native_sources'):
        from shared_bootstrap import resolve_export
        if not any(m['name'].lower()=='litoddraw.dll' for m in modules):
            readiness['wait_reason']='native_source_session_not_loaded'
            return None
        source_session=resolve_export(modules,'litoddraw.dll','LiToSourceSession')
        if read_process(process_handle,source_session,8)!=struct.pack('<II',0x31534C53,1):
            raise RuntimeError('Native source-session ABI mismatch')

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate an x86 connect redirect page")

    call_count_address = remote + 0x800
    original_ip_address = remote + 0x804
    original_port_address = remote + 0x808
    credential_valid_address = remote + 0x810
    rental_transfer_auto_enter_address = remote + 0x814
    credential_account_address = remote + 0x820
    credential_password_address = remote + 0x840
    credential_character_address = remote + 0x850
    stub = bytearray()
    jump_fixups: list[int] = []
    credential_rearm_call: int | None = None
    stub += b"\x8B\x44\x24\x08"  # mov eax,[esp+8] (sockaddr*)
    stub += b"\x85\xC0"  # test eax,eax
    stub += b"\x0F\x84\x00\x00\x00\x00"; jump_fixups.append(len(stub) - 4)
    stub += b"\x83\x7C\x24\x0C\x08"  # cmp dword [esp+0c],8
    stub += b"\x0F\x8C\x00\x00\x00\x00"; jump_fixups.append(len(stub) - 4)
    stub += b"\x66\x83\x38\x02"  # cmp word [eax],AF_INET
    stub += b"\x0F\x85\x00\x00\x00\x00"; jump_fixups.append(len(stub) - 4)
    # Preserve an in-protocol redirect to another loopback address.  The
    # launcher only needs to replace the client's original public endpoint;
    # LiTo uses a distinct login and VS/game address even when both services
    # live on the same machine.
    stub += b"\x80\x78\x04\x7F"  # cmp byte [eax+4],127
    # not loopback: continue with the public-endpoint rewrite after the four
    # volatile state stores, optional credential rearm call, and the two-byte
    # passthrough jump.  Resolve this from the actual telemetry label instead
    # of preserving the old fixed 0x1E distance.
    stub += b"\x0F\x85\x00\x00\x00\x00"
    public_rewrite_jump = len(stub) - 4
    # The original public login endpoint is rewritten to profile.host.  A
    # reconnect to that same loopback address can occur before the physical
    # login form has completed.  In manual selection, setting 0x75F755 there
    # makes sub_48FAA0 take its cached C35 branch and bypass the source modal.
    # Keep it clear only for the rewritten LOGIN address; an in-protocol
    # redirect to a different loopback address still needs the authenticated
    # auto-login handoff on the VS/GAME endpoint.
    auto_login_value: int | str = 1
    if manual_native_selection:
        auto_login_value = "login-target=0, in-protocol-handoff=1"
        stub += b"\x81\x78\x04" + bytes(profile["packed_ip"])
        stub += b"\x75\x09"  # jne handoff_auto_login
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_AUTO_LOGIN_FLAG) + b"\x00"
        stub += b"\xEB\x07"  # jmp auto_login_done
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_AUTO_LOGIN_FLAG) + b"\x01"
    else:
        stub += (
            b"\xC6\x05"
            + struct.pack("<I", CLIENT_AUTO_LOGIN_FLAG)
            + b"\x01"
        )
    auto_enter_value: int | str = 1
    game_ui_value = 0
    stub += b"\xC6\x05" + struct.pack("<I", CLIENT_VS_AUTO_SELECT_SENT_FLAG) + b"\x00"
    if manual_native_selection:
        # Preserve sub_4A7550's ordinary S_CB row parser and its native click
        # callbacks.  This prevents the launcher-only shortcut from sending an
        # empty C_C9; it does not synthesize a row, select a character, or emit
        # a packet.
        assert (
            CLIENT_CHARACTER_AUTO_SELECT_SOURCE_B
            == CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A + 1
        )
        stub += (
            b"\x66\xC7\x05"
            + struct.pack("<I", CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A)
            + b"\x00\x00"
        )
        # A group-2 rental transfer reconnects while the old town world still
        # owns phase 4. The standalone binary builds the new page-7 directory,
        # but its renderer keeps painting the old loading/world state unless
        # the official launcher's endpoint handoff clears this scalar. Reset
        # only the phase; LiTo replaces the old world object in its own native
        # transition after the physical character selection.
        stub += (
            b"\xC7\x05"
            + struct.pack("<I", CLIENT_PHASE)
            + b"\x00\x00\x00\x00"
        )
        # Keep CLIENT_TOURNAMENT_WORLD_ROOT plus CLIENT_WORLD_SCENE and
        # CLIENT_STATIC_SCENE: the destination S_WORLD updates these reusable
        # native scene containers. Clearing them leaves phase 4 active but no
        # renderer-owned world behind the room's final frame.
        # The arena controller, however, owns the UI pointers cleared below.
        # Null it as one ownership unit so the next
        # native READY chooses sub_47CF00 (constructor), not sub_47E360
        # (refresh), and recreates dword_64B908 for S_D2.
        stub += (
            b"\xC7\x05"
            + struct.pack("<I", CLIENT_TOURNAMENT_READY_WORLD_CONTROLLER)
            + b"\x00\x00\x00\x00"
        )
        for ui_scene_pointer in CLIENT_ENDPOINT_UI_SCENE_POINTERS:
            stub += (
                b"\xC7\x05"
                + struct.pack("<I", ui_scene_pointer)
                + b"\x00\x00\x00\x00"
            )
        for arena_dimension, arena_value in (
            (CLIENT_ARENA_START_GLOBALS["arena_width"], CLIENT_CHARACTER_PAGE_ARENA_WIDTH),
            (CLIENT_ARENA_START_GLOBALS["arena_height"], CLIENT_CHARACTER_PAGE_ARENA_HEIGHT),
        ):
            stub += (
                b"\xC7\x05"
                + struct.pack("<I", arena_dimension)
                + struct.pack("<I", arena_value)
            )
    # Reuse LiTo's native auto-enter branch without launching the executable
    # itself in the unrelated `lteventsvr` process role.  A physical source
    # login must keep the selector interactive.  The original transfer-stone
    # producer later arms this process-private marker, so only the destination
    # LOGIN/GAME pair accepts the already-selected character and emits C_CA(0).
    if manual_native_selection:
        auto_enter_value = "0-until-rental-transfer-then-1"
        stub += b"\x83\x3D" + struct.pack("<I", rental_transfer_auto_enter_address) + b"\x00"
        stub += b"\x74\x09"
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG) + b"\x01"
        stub += b"\xEB\x07"
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG) + b"\x00"
    else:
        stub += b"\xC6\x05" + struct.pack("<I", CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG) + b"\x01"
    stub += b"\xC6\x05" + struct.pack("<I", CLIENT_GAME_UI_ACTIVE_FLAG) + bytes((game_ui_value,))
    # The connect hook already normalizes the legacy auto-login/UI flags for
    # this modern loopback handoff.  Normalize the stale news-defer byte too,
    # including manual native selection: this does not select a character or
    # channel, it only makes the exact native S_E6 receiver take its immediate
    # opcode-only C_E2 producer instead of an invalid old-UI callback path.
    stub += b"\xC6\x05" + struct.pack("<I", CLIENT_LITO_NEWS_DEFER_FLAG) + b"\x00"
    if launcher_credentials is not None or manual_native_selection:
        # Refill the launcher's plain cells at connect() time, after LiTo has
        # initialized its login form but before it consumes the auto-login
        # flag. In manual mode the first native login callback captures the
        # physical account/password bytes into this process-private page; no
        # credential needs to cross back into Python. A rel32 call keeps the
        # copier outside all early rel8 passthrough branches.
        credential_rearm_call = len(stub)
        stub += b"\xE8\x00\x00\x00\x00"
    stub += b"\xE9\x00\x00\x00\x00"; jump_fixups.append(len(stub) - 4)
    telemetry_offset = len(stub)
    stub += b"\xF0\xFF\x05" + struct.pack("<I", call_count_address)
    stub += b"\x8B\x48\x04"  # mov ecx,[eax+4]
    stub += b"\x89\x0D" + struct.pack("<I", original_ip_address)
    stub += b"\x66\x8B\x48\x02"  # mov cx,[eax+2]
    stub += b"\x66\x89\x0D" + struct.pack("<I", original_port_address)
    stub += b"\x66\xC7\x40\x02" + int(profile["port"]).to_bytes(2, "big")
    if source_session:
        stub += b"\x8B\x0D" + struct.pack('<I',source_session+16)  # chosen host-order port
        stub += b"\x85\xC9\x74\x06\x86\xCD\x66\x89\x48\x02"

    stub += b"\xC7\x40\x04" + bytes(profile["packed_ip"])
    passthrough_offset = len(stub)
    stub += b"\xB8" + struct.pack("<I", original_connect)  # mov eax,connect
    stub += b"\xFF\xE0"  # jmp eax
    for displacement_index in jump_fixups:
        displacement = passthrough_offset - (displacement_index + 4)
        struct.pack_into("<i", stub, displacement_index, displacement)
    public_rewrite_displacement = telemetry_offset - (public_rewrite_jump + 4)
    struct.pack_into("<i", stub, public_rewrite_jump, public_rewrite_displacement)

    if launcher_credentials is not None or manual_native_selection:
        rearm_offset = len(stub)
        stub += b"\x83\x3D" + struct.pack("<I", credential_valid_address) + b"\x01"
        rearm_skip = len(stub)
        stub += b"\x75\x00"
        stub += b"\x56\x57\x51"  # push esi; push edi; push ecx
        credential_rearm_regions = [
            (credential_account_address, CLIENT_ACCOUNT_CELLS, CLIENT_ACCOUNT_CELL_COUNT),
            (credential_password_address, CLIENT_PASSWORD_CELLS, CLIENT_PASSWORD_CELL_COUNT),
        ]
        # A physically selected character remains owned by LiTo's native
        # buffers. Only an explicit launcher-supplied character is restored;
        # an empty backup must never erase a later native selection during a
        # rental-town -> battle reconnect.
        if launcher_credentials is not None and launcher_credentials[2]:
            credential_rearm_regions.append(
                (credential_character_address, CLIENT_CHARACTER_NAME,
                 CLIENT_CHARACTER_NAME_CAPACITY)
            )
        for source, target, size in credential_rearm_regions:
            stub += b"\xBE" + struct.pack("<I", source)
            stub += b"\xBF" + struct.pack("<I", target)
            stub += b"\xB9" + struct.pack("<I", size)
            stub += b"\xF3\xA4"  # rep movsb
        stub += b"\x59\x5F\x5E"  # pop ecx; pop edi; pop esi
        rearm_done = len(stub)
        stub += b"\xC3"
        rearm_displacement = rearm_done - (rearm_skip + 2)
        if not -128 <= rearm_displacement <= 127:
            raise RuntimeError("connect credential rearm branch exceeded rel8 range")
        stub[rearm_skip + 1] = rearm_displacement & 0xFF
        assert credential_rearm_call is not None
        struct.pack_into(
            "<i",
            stub,
            credential_rearm_call + 1,
            rearm_offset - (credential_rearm_call + 5),
        )

    native_login_capture: dict[str, Any] | None = None
    native_login_capture_sites: list[tuple[int, bytes, bytes]] = []
    if manual_native_selection:
        # sub_492090 has decoded the native edit controls into bounded stack
        # strings by either send call. Capture them in the same process, then
        # tail-jump to the untouched writer so the original argument stack and
        # return address remain intact. The bytes never enter this report.
        capture_offset = len(stub)
        stub += b"\x9C\x60\xFC"  # pushfd; pushad; cld for bounded forward copies
        stub += b"\xC7\x05" + struct.pack("<I", credential_valid_address) + b"\x00\x00\x00\x00"
        stub += b"\x8B\x74\x24\x30"  # original account pointer
        stub += b"\xBF" + struct.pack("<I", credential_account_address)
        stub += b"\xB9" + struct.pack("<I", CLIENT_ACCOUNT_CELL_COUNT)
        stub += b"\xF3\xA4"
        stub += b"\x8B\x74\x24\x34"  # original password pointer
        stub += b"\xBF" + struct.pack("<I", credential_password_address)
        stub += b"\xB9" + struct.pack("<I", CLIENT_PASSWORD_CELL_COUNT)
        stub += b"\xF3\xA4"
        capture_invalid_fixups: list[int] = []
        for address, branch_opcode in (
            (credential_account_address, b"\x74"),
            (credential_password_address, b"\x74"),
            (credential_account_address + CLIENT_ACCOUNT_CELL_COUNT - 1, b"\x75"),
            (credential_password_address + CLIENT_PASSWORD_CELL_COUNT - 1, b"\x75"),
        ):
            stub += b"\x80\x3D" + struct.pack("<I", address) + b"\x00"
            stub += branch_opcode + b"\x00"
            capture_invalid_fixups.append(len(stub) - 1)
        stub += b"\xC7\x05" + struct.pack("<I", credential_valid_address) + b"\x01\x00\x00\x00"
        capture_done = len(stub)
        for displacement_index in capture_invalid_fixups:
            displacement = capture_done - (displacement_index + 1)
            if not -128 <= displacement <= 127:
                raise RuntimeError("native login capture validation exceeded rel8 range")
            stub[displacement_index] = displacement & 0xFF
        stub += b"\x61\x9D"  # popad; popfd
        stub += b"\xB8" + struct.pack("<I", CLIENT_PACKET_WRITER)
        stub += b"\xFF\xE0"  # tail-jump to the original writer
        capture_address = remote + capture_offset
        for call_site in (
            CLIENT_NATIVE_LOGIN_CONNECTOR_SEND,
            CLIENT_NATIVE_LOGIN_DIRECT_SEND,
        ):
            observed_call = read_process(process_handle, call_site, 5)
            if len(observed_call) != 5 or observed_call[0] != 0xE8:
                raise RuntimeError(
                    f"native login writer call changed at 0x{call_site:08X}: "
                    + observed_call.hex(" ").upper()
                )
            observed_target = (
                call_site + 5 + struct.unpack("<i", observed_call[1:])[0]
            ) & 0xFFFFFFFF
            if observed_target != CLIENT_PACKET_WRITER:
                raise RuntimeError(
                    f"native login writer target changed at 0x{call_site:08X}: "
                    f"0x{observed_target:08X}"
                )
            patched_call = b"\xE8" + struct.pack(
                "<i", capture_address - (call_site + 5)
            )
            native_login_capture_sites.append(
                (call_site, observed_call, patched_call)
            )
        native_login_capture = {
            "status": "installed",
            "capture": f"0x{capture_address:08X}",
            "credential_valid_marker": f"0x{credential_valid_address:08X}",
            "call_sites": [
                {
                    "address": f"0x{address:08X}",
                    "observed": observed.hex(" ").upper(),
                    "patch": patch.hex(" ").upper(),
                }
                for address, observed, patch in native_login_capture_sites
            ],
            "account_capacity": CLIENT_ACCOUNT_CELL_COUNT,
            "password_capacity": CLIENT_PASSWORD_CELL_COUNT,
            "credentials_logged": False,
            "credential_memory_scope": "LiTo process lifetime only",
            "packet_writer_preserved": f"0x{CLIENT_PACKET_WRITER:08X}",
            "user_input_synthesized": False,
            "executable_modified": False,
        }

    if len(stub) >= 0x800:
        raise RuntimeError("connect redirect credential bridge exceeded code region")
    data = bytearray(b"\x90" * 0x880)
    data[: len(stub)] = stub
    data[0x800 : 0x880] = b"\x00" * 0x80
    if launcher_credentials is not None:
        for offset, secret, capacity in (
            (0x820, launcher_credentials[0], CLIENT_ACCOUNT_CELL_COUNT),
            (0x840, launcher_credentials[1], CLIENT_PASSWORD_CELL_COUNT),
            (0x850, launcher_credentials[2], CLIENT_CHARACTER_NAME_CAPACITY),
        ):
            value = bytes(secret)[: capacity - 1]
            data[offset : offset + len(value)] = value
            data[offset + len(value)] = 0
        struct.pack_into("<I", data, 0x810, 1)
    data = bytes(data)
    written = write_process(process_handle, remote, data)
    if written != len(data):
        raise RuntimeError("short write while installing connect redirect")
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(remote), len(stub)
    ):
        raise win_error("FlushInstructionCache(connect redirect)")
    thunk_patch = (
        b"\xE9"
        + struct.pack("<i", remote - (CONNECT_DIRECT_THUNK + 5))
    )
    write_process_protected(process_handle, CONNECT_DIRECT_THUNK, thunk_patch)
    write_process_protected(
        process_handle, CONNECT_IAT_SLOT, struct.pack("<I", remote)
    )
    for call_site, _observed, patched_call in native_login_capture_sites:
        write_process_protected(process_handle, call_site, patched_call)
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(CONNECT_DIRECT_THUNK), len(thunk_patch)
    ):
        raise win_error("FlushInstructionCache(connect thunk)")
    for call_site, _observed, patched_call in native_login_capture_sites:
        if not kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(call_site), len(patched_call)
        ):
            raise win_error("FlushInstructionCache(native login capture)")
    if source_session:
        write_process(process_handle,source_session+8,struct.pack('<I',remote))
    return {
        "status": "installed",
        "native_source_session_address": source_session,
        "selection_flow": (
            "native_manual" if manual_native_selection else "compatibility_automatic"
        ),
        "handoff_flags": {
            "auto_login": auto_login_value,
            "auto_enter_channel": auto_enter_value,
            "game_ui_active": game_ui_value,
            "news_defer": 0,
            "character_auto_select_source_a": (
                0 if manual_native_selection else "preserved"
            ),
            "character_auto_select_source_b": (
                0 if manual_native_selection else "preserved"
            ),
            "client_phase": (
                "reset-to-0-on-native-endpoint-connect"
                if manual_native_selection
                else "preserved"
            ),
            "scene_roots": (
                "arena-controller-ui-cleared-and-page-layout-restored-with-native-world-roots-preserved"
                if manual_native_selection
                else "preserved"
            ),
        },
        "credentials_rearmed_on_connect": launcher_credentials is not None,
        "native_physical_credentials_rearmed_on_connect": manual_native_selection,
        "native_login_credential_capture": native_login_capture,
        "credential_lengths": (
            {
                "account": len(launcher_credentials[0]),
                "password": len(launcher_credentials[1]),
                "character": len(launcher_credentials[2]),
            }
            if launcher_credentials is not None
            else None
        ),
        "iat_slot": f"0x{CONNECT_IAT_SLOT:08X}",
        "iat_original_connect": f"0x{iat_original_connect:08X}",
        "connect_routine": f"0x{CONNECT_ROUTINE:08X}",
        "direct_connect_thunk": f"0x{CONNECT_DIRECT_THUNK:08X}",
        "direct_thunk_layout": thunk_layout,
        "direct_thunk_observed_bytes": observed_thunk.hex(" ").upper(),
        "direct_thunk_original_bytes": observed_thunk[:5].hex(" ").upper(),
        "direct_thunk_patch": thunk_patch.hex(" ").upper(),
        "original_connect": f"0x{original_connect:08X}",
        "original_connect_module": socket_module["name"],
        "connect_routine_signature_observed": read_process(
            process_handle, CONNECT_ROUTINE, len(CONNECT_ROUTINE_SIGNATURE)
        ).hex(" ").upper(),
        "hook_address": f"0x{remote:08X}",
        "call_count_address": f"0x{call_count_address:08X}",
        "telemetry_address": f"0x{call_count_address:08X}",
        "credential_valid_address": f"0x{credential_valid_address:08X}",
        "rental_transfer_auto_enter_address": (
            f"0x{rental_transfer_auto_enter_address:08X}"
        ),
        "stub_size": len(stub),
        "profile": {
            key: value
            for key, value in profile.items()
            if key != "packed_ip"
        },
    }


def update_connect_redirect_telemetry(
    process_handle: int, redirect: dict[str, Any]
) -> None:
    telemetry_address = int(redirect["telemetry_address"], 16)
    data = read_process(process_handle, telemetry_address, 12)
    if len(data) != 12:
        return
    call_count = struct.unpack_from("<I", data, 0)[0]
    telemetry: dict[str, Any] = {"call_count": call_count}
    credential_valid_address = redirect.get("credential_valid_address")
    if credential_valid_address is not None:
        valid_raw = read_process(
            process_handle, int(str(credential_valid_address), 16), 4
        )
        telemetry["native_login_credentials_captured"] = (
            len(valid_raw) == 4 and struct.unpack("<I", valid_raw)[0] == 1
        )
    if call_count:
        telemetry["last_original_host"] = str(ipaddress.IPv4Address(data[4:8]))
        telemetry["last_original_port"] = int.from_bytes(data[8:10], "big")
        telemetry["redirected_host"] = redirect["profile"]["host"]
        telemetry["redirected_port"] = redirect["profile"]["port"]
    current_slot = read_process(process_handle, CONNECT_IAT_SLOT, 4)
    hook_address = int(redirect["hook_address"], 16)
    iat_original_connect = int(redirect["iat_original_connect"], 16)
    current_pointer = (
        struct.unpack("<I", current_slot)[0] if len(current_slot) == 4 else None
    )
    if current_pointer == iat_original_connect:
        write_process_protected(
            process_handle, CONNECT_IAT_SLOT, struct.pack("<I", hook_address)
        )
        redirect["reapplied_count"] = int(redirect.get("reapplied_count", 0)) + 1
        current_pointer = hook_address
    telemetry["iat_hook_active"] = current_pointer == hook_address
    if current_pointer is not None:
        telemetry["iat_slot_value"] = f"0x{current_pointer:08X}"
    expected_thunk = bytes.fromhex(redirect["direct_thunk_patch"])
    original_thunk = bytes.fromhex(redirect["direct_thunk_original_bytes"])
    current_thunk = read_process(
        process_handle, CONNECT_DIRECT_THUNK, len(expected_thunk)
    )
    if current_thunk == original_thunk:
        write_process_protected(process_handle, CONNECT_DIRECT_THUNK, expected_thunk)
        kernel32.FlushInstructionCache(
            process_handle, ctypes.c_void_p(CONNECT_DIRECT_THUNK), len(expected_thunk)
        )
        redirect["reapplied_count"] = int(redirect.get("reapplied_count", 0)) + 1
        current_thunk = expected_thunk
    telemetry["direct_thunk_hook_active"] = current_thunk == expected_thunk
    telemetry["direct_thunk_value"] = current_thunk.hex(" ").upper()
    telemetry["connect_path_hook_active"] = telemetry["direct_thunk_hook_active"]
    redirect["telemetry"] = telemetry


def install_window_close_trace(process_handle: int, pid: int) -> dict[str, Any] | None:
    """Record DestroyWindow and the client's direct PostQuitMessage thunk.

    The DestroyWindow hook preserves the original call.  The PostQuitMessage hook
    is installed only after the first connect and suppresses the quit so the
    caller and resulting client state remain readable for diagnostics.
    """
    pointer_bytes = read_process(process_handle, DESTROY_WINDOW_IAT_SLOT, 4)
    post_quit_pointer_bytes = read_process(
        process_handle, POST_QUIT_MESSAGE_IAT_SLOT, 4
    )
    if len(pointer_bytes) != 4 or len(post_quit_pointer_bytes) != 4:
        return None
    original_destroy_window = struct.unpack("<I", pointer_bytes)[0]
    post_quit_iat_target = struct.unpack("<I", post_quit_pointer_bytes)[0]
    reference_call = read_process(process_handle, POST_QUIT_MESSAGE_REFERENCE_CALL, 5)
    if len(reference_call) == 5 and reference_call[0] == 0xE8:
        original_post_quit_message = (
            POST_QUIT_MESSAGE_REFERENCE_CALL
            + 5
            + struct.unpack("<i", reference_call[1:5])[0]
        ) & 0xFFFFFFFF
    else:
        original_post_quit_message = post_quit_iat_target
    if not original_destroy_window or not original_post_quit_message:
        return None

    ntuser_call_one_param = remote_export_address(
        pid, "win32u.dll", "NtUserCallOneParam"
    )
    ntuser_post_message = remote_export_address(
        pid, "win32u.dll", "NtUserPostMessage"
    )
    ntuser_post_thread_message = remote_export_address(
        pid, "win32u.dll", "NtUserPostThreadMessage"
    )
    ntuser_call_one_param_original = read_process(
        process_handle, ntuser_call_one_param, 5
    )
    ntuser_post_message_original = read_process(
        process_handle, ntuser_post_message, 5
    )
    ntuser_post_thread_message_original = read_process(
        process_handle, ntuser_post_thread_message, 5
    )
    if (
        len(ntuser_call_one_param_original) != 5
        or len(ntuser_post_message_original) != 5
        or len(ntuser_post_thread_message_original) != 5
    ):
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote or remote > 0xFFFFFFFF:
        raise RuntimeError("could not allocate an x86 window-close trace page")

    destroy_call_count_address = remote + 0x200
    destroy_caller_address = remote + 0x204
    destroy_hwnd_address = remote + 0x208
    quit_call_count_address = remote + 0x210
    quit_caller_address = remote + 0x214
    quit_exit_code_address = remote + 0x218
    one_param_call_count_address = remote + 0x220
    one_param_caller_address = remote + 0x224
    one_param_value_address = remote + 0x228
    one_param_routine_address = remote + 0x22C
    post_message_call_count_address = remote + 0x230
    post_message_caller_address = remote + 0x234
    post_message_hwnd_address = remote + 0x238
    post_message_value_address = remote + 0x23C
    post_message_wparam_address = remote + 0x240
    post_message_lparam_address = remote + 0x244
    post_thread_call_count_address = remote + 0x250
    post_thread_caller_address = remote + 0x254
    post_thread_id_address = remote + 0x258
    post_thread_message_address = remote + 0x25C
    post_thread_wparam_address = remote + 0x260
    post_thread_lparam_address = remote + 0x264

    destroy_stub = bytearray()
    destroy_stub += b"\x8B\x04\x24"  # mov eax,[esp] (caller return address)
    destroy_stub += b"\xA3" + struct.pack("<I", destroy_caller_address)
    destroy_stub += b"\x8B\x44\x24\x04"  # mov eax,[esp+4] (HWND)
    destroy_stub += b"\xA3" + struct.pack("<I", destroy_hwnd_address)
    destroy_stub += b"\xF0\xFF\x05" + struct.pack("<I", destroy_call_count_address)
    destroy_stub += b"\xB8" + struct.pack("<I", original_destroy_window)
    destroy_stub += b"\xFF\xE0"  # jmp eax; preserve stdcall cleanup/return value

    quit_stub_address = remote + 0x40
    quit_stub = bytearray()
    quit_stub += b"\x8B\x04\x24"  # mov eax,[esp] (caller return address)
    quit_stub += b"\xA3" + struct.pack("<I", quit_caller_address)
    quit_stub += b"\x8B\x44\x24\x04"  # mov eax,[esp+4] (exit code)
    quit_stub += b"\xA3" + struct.pack("<I", quit_exit_code_address)
    quit_stub += b"\xF0\xFF\x05" + struct.pack("<I", quit_call_count_address)
    quit_stub += b"\x33\xC0\xC2\x04\x00"  # xor eax,eax; ret 4

    one_param_stub_address = remote + 0x80
    one_param_trampoline_address = remote + 0x180
    one_param_stub = bytearray()
    one_param_stub += b"\x83\x7C\x24\x08\x3B"  # cmp dword [esp+8],3Bh
    one_param_pass_fixup = len(one_param_stub) + 1
    one_param_stub += b"\x75\x00"  # jne pass-through
    one_param_stub += b"\x8B\x04\x24\xA3" + struct.pack(
        "<I", one_param_caller_address
    )
    one_param_stub += b"\x8B\x44\x24\x04\xA3" + struct.pack(
        "<I", one_param_value_address
    )
    one_param_stub += b"\x8B\x44\x24\x08\xA3" + struct.pack(
        "<I", one_param_routine_address
    )
    one_param_stub += b"\xF0\xFF\x05" + struct.pack(
        "<I", one_param_call_count_address
    )
    one_param_stub += b"\x33\xC0\xC2\x08\x00"  # suppress PostQuit; ret 8
    one_param_pass_offset = len(one_param_stub)
    one_param_stub += b"\xB8" + struct.pack("<I", one_param_trampoline_address)
    one_param_stub += b"\xFF\xE0"
    one_param_stub[one_param_pass_fixup] = (
        one_param_pass_offset - (one_param_pass_fixup + 1)
    ) & 0xFF

    post_message_stub_address = remote + 0xC0
    post_message_trampoline_address = remote + 0x1A0
    post_message_stub = bytearray()
    post_message_stub += b"\x83\x7C\x24\x08\x12"  # cmp message,WM_QUIT
    post_message_pass_fixup = len(post_message_stub) + 1
    post_message_stub += b"\x75\x00"  # jne pass-through
    for stack_offset, destination in (
        (0x00, post_message_caller_address),
        (0x04, post_message_hwnd_address),
        (0x08, post_message_value_address),
        (0x0C, post_message_wparam_address),
        (0x10, post_message_lparam_address),
    ):
        if stack_offset == 0:
            post_message_stub += b"\x8B\x04\x24"
        else:
            post_message_stub += b"\x8B\x44\x24" + bytes([stack_offset])
        post_message_stub += b"\xA3" + struct.pack("<I", destination)
    post_message_stub += b"\xF0\xFF\x05" + struct.pack(
        "<I", post_message_call_count_address
    )
    post_message_stub += b"\xB8\x01\x00\x00\x00\xC2\x10\x00"
    post_message_pass_offset = len(post_message_stub)
    post_message_stub += b"\xB8" + struct.pack(
        "<I", post_message_trampoline_address
    )
    post_message_stub += b"\xFF\xE0"
    post_message_stub[post_message_pass_fixup] = (
        post_message_pass_offset - (post_message_pass_fixup + 1)
    ) & 0xFF

    post_thread_stub_address = remote + 0x120
    post_thread_trampoline_address = remote + 0x1C0
    post_thread_stub = bytearray()
    post_thread_stub += b"\x83\x7C\x24\x08\x12"  # cmp message,WM_QUIT
    post_thread_pass_fixup = len(post_thread_stub) + 1
    post_thread_stub += b"\x75\x00"
    for stack_offset, destination in (
        (0x00, post_thread_caller_address),
        (0x04, post_thread_id_address),
        (0x08, post_thread_message_address),
        (0x0C, post_thread_wparam_address),
        (0x10, post_thread_lparam_address),
    ):
        if stack_offset == 0:
            post_thread_stub += b"\x8B\x04\x24"
        else:
            post_thread_stub += b"\x8B\x44\x24" + bytes([stack_offset])
        post_thread_stub += b"\xA3" + struct.pack("<I", destination)
    post_thread_stub += b"\xF0\xFF\x05" + struct.pack(
        "<I", post_thread_call_count_address
    )
    post_thread_stub += b"\xB8\x01\x00\x00\x00\xC2\x10\x00"
    post_thread_pass_offset = len(post_thread_stub)
    post_thread_stub += b"\xB8" + struct.pack(
        "<I", post_thread_trampoline_address
    )
    post_thread_stub += b"\xFF\xE0"
    post_thread_stub[post_thread_pass_fixup] = (
        post_thread_pass_offset - (post_thread_pass_fixup + 1)
    ) & 0xFF

    one_param_trampoline = ntuser_call_one_param_original + b"\xE9" + struct.pack(
        "<i",
        (ntuser_call_one_param + 5) - (one_param_trampoline_address + 10),
    )
    post_message_trampoline = ntuser_post_message_original + b"\xE9" + struct.pack(
        "<i", (ntuser_post_message + 5) - (post_message_trampoline_address + 10)
    )
    post_thread_trampoline = ntuser_post_thread_message_original + b"\xE9" + struct.pack(
        "<i",
        (ntuser_post_thread_message + 5) - (post_thread_trampoline_address + 10),
    )

    code = bytearray(b"\x90" * 0x200)
    for offset, blob in (
        (0x000, bytes(destroy_stub)),
        (0x040, bytes(quit_stub)),
        (0x080, bytes(one_param_stub)),
        (0x0C0, bytes(post_message_stub)),
        (0x120, bytes(post_thread_stub)),
        (0x180, one_param_trampoline),
        (0x1A0, post_message_trampoline),
        (0x1C0, post_thread_trampoline),
    ):
        if offset + len(blob) > len(code):
            raise RuntimeError("window-close trace stub layout exceeded its page")
        code[offset : offset + len(blob)] = blob
    data = bytes(code) + b"\x00" * 0x70
    if write_process(process_handle, remote, data) != len(data):
        raise RuntimeError("short write while installing window-close trace")
    if not kernel32.FlushInstructionCache(
        process_handle, ctypes.c_void_p(remote), len(code)
    ):
        raise win_error("FlushInstructionCache(window-close trace)")
    write_process_protected(
        process_handle, DESTROY_WINDOW_IAT_SLOT, struct.pack("<I", remote)
    )
    original_post_quit_bytes = read_process(process_handle, original_post_quit_message, 5)
    if len(original_post_quit_bytes) != 5:
        return None
    post_quit_patch = b"\xE9" + struct.pack(
        "<i", quit_stub_address - (original_post_quit_message + 5)
    )
    write_process_protected(
        process_handle, original_post_quit_message, post_quit_patch
    )
    one_param_patch = b"\xE9" + struct.pack(
        "<i", one_param_stub_address - (ntuser_call_one_param + 5)
    )
    post_message_patch = b"\xE9" + struct.pack(
        "<i", post_message_stub_address - (ntuser_post_message + 5)
    )
    post_thread_patch = b"\xE9" + struct.pack(
        "<i", post_thread_stub_address - (ntuser_post_thread_message + 5)
    )
    write_process_protected(
        process_handle, ntuser_call_one_param, one_param_patch
    )
    write_process_protected(process_handle, ntuser_post_message, post_message_patch)
    write_process_protected(
        process_handle, ntuser_post_thread_message, post_thread_patch
    )
    return {
        "status": "installed",
        "iat_slot": f"0x{DESTROY_WINDOW_IAT_SLOT:08X}",
        "original_destroy_window": f"0x{original_destroy_window:08X}",
        "post_quit_iat_slot": f"0x{POST_QUIT_MESSAGE_IAT_SLOT:08X}",
        "post_quit_iat_target": f"0x{post_quit_iat_target:08X}",
        "post_quit_reference_call": f"0x{POST_QUIT_MESSAGE_REFERENCE_CALL:08X}",
        "post_quit_reference_call_bytes": reference_call.hex(" ").upper(),
        "post_quit_target": f"0x{original_post_quit_message:08X}",
        "post_quit_hook_address": f"0x{quit_stub_address:08X}",
        "post_quit_original_bytes": original_post_quit_bytes.hex(" ").upper(),
        "post_quit_patch": post_quit_patch.hex(" ").upper(),
        "hook_address": f"0x{remote:08X}",
        "telemetry_address": f"0x{destroy_call_count_address:08X}",
        "post_quit_telemetry_address": f"0x{quit_call_count_address:08X}",
        "ntuser_call_one_param": f"0x{ntuser_call_one_param:08X}",
        "ntuser_call_one_param_original": ntuser_call_one_param_original.hex(" ").upper(),
        "ntuser_call_one_param_patch": one_param_patch.hex(" ").upper(),
        "ntuser_call_one_param_telemetry_address": f"0x{one_param_call_count_address:08X}",
        "ntuser_post_message": f"0x{ntuser_post_message:08X}",
        "ntuser_post_message_original": ntuser_post_message_original.hex(" ").upper(),
        "ntuser_post_message_patch": post_message_patch.hex(" ").upper(),
        "ntuser_post_message_telemetry_address": f"0x{post_message_call_count_address:08X}",
        "ntuser_post_thread_message": f"0x{ntuser_post_thread_message:08X}",
        "ntuser_post_thread_message_original": ntuser_post_thread_message_original.hex(" ").upper(),
        "ntuser_post_thread_message_patch": post_thread_patch.hex(" ").upper(),
        "ntuser_post_thread_message_telemetry_address": f"0x{post_thread_call_count_address:08X}",
        "stub_size": len(code),
        "suppresses_close": False,
        "suppresses_quit": True,
        "executable_modified": False,
    }


def update_window_close_trace(process_handle: int, trace: dict[str, Any]) -> None:
    post_quit_target = int(trace["post_quit_target"], 16)
    post_quit_patch = bytes.fromhex(trace["post_quit_patch"])
    post_quit_original = bytes.fromhex(trace["post_quit_original_bytes"])
    current_post_quit_bytes = read_process(
        process_handle, post_quit_target, len(post_quit_patch)
    )
    if current_post_quit_bytes == post_quit_original:
        write_process_protected(process_handle, post_quit_target, post_quit_patch)
        trace["post_quit_reapplied_count"] = int(
            trace.get("post_quit_reapplied_count", 0)
        ) + 1
        current_post_quit_bytes = post_quit_patch
    destroy_data = read_process(
        process_handle, int(trace["telemetry_address"], 16), 12
    )
    quit_data = read_process(
        process_handle, int(trace["post_quit_telemetry_address"], 16), 12
    )
    one_param_data = read_process(
        process_handle,
        int(trace["ntuser_call_one_param_telemetry_address"], 16),
        16,
    )
    post_message_data = read_process(
        process_handle,
        int(trace["ntuser_post_message_telemetry_address"], 16),
        24,
    )
    post_thread_data = read_process(
        process_handle,
        int(trace["ntuser_post_thread_message_telemetry_address"], 16),
        24,
    )
    if (
        len(destroy_data) != 12
        or len(quit_data) != 12
        or len(one_param_data) != 16
        or len(post_message_data) != 24
        or len(post_thread_data) != 24
    ):
        return
    call_count, caller, hwnd = struct.unpack("<3I", destroy_data)
    quit_call_count, quit_caller, quit_exit_code = struct.unpack("<3I", quit_data)
    one_param_count, one_param_caller, one_param_value, one_param_routine = (
        struct.unpack("<4I", one_param_data)
    )
    (
        post_message_count,
        post_message_caller,
        post_message_hwnd,
        post_message_value,
        post_message_wparam,
        post_message_lparam,
    ) = struct.unpack("<6I", post_message_data)
    (
        post_thread_count,
        post_thread_caller,
        post_thread_id,
        post_thread_message,
        post_thread_wparam,
        post_thread_lparam,
    ) = struct.unpack("<6I", post_thread_data)
    trace["telemetry"] = {
        "destroy_window_call_count": call_count,
        "last_destroy_window_caller": f"0x{caller:08X}",
        "last_destroy_window_hwnd": f"0x{hwnd:08X}",
        "post_quit_call_count": quit_call_count,
        "last_post_quit_caller": f"0x{quit_caller:08X}",
        "last_post_quit_exit_code": f"0x{quit_exit_code:08X}",
        "post_quit_hook_active": current_post_quit_bytes == post_quit_patch,
        "post_quit_target_bytes": current_post_quit_bytes.hex(" ").upper(),
        "ntuser_call_one_param_quit_count": one_param_count,
        "ntuser_call_one_param_caller": f"0x{one_param_caller:08X}",
        "ntuser_call_one_param_value": f"0x{one_param_value:08X}",
        "ntuser_call_one_param_routine": f"0x{one_param_routine:08X}",
        "ntuser_post_message_quit_count": post_message_count,
        "ntuser_post_message_caller": f"0x{post_message_caller:08X}",
        "ntuser_post_message_hwnd": f"0x{post_message_hwnd:08X}",
        "ntuser_post_message_message": f"0x{post_message_value:08X}",
        "ntuser_post_message_wparam": f"0x{post_message_wparam:08X}",
        "ntuser_post_message_lparam": f"0x{post_message_lparam:08X}",
        "ntuser_post_thread_message_quit_count": post_thread_count,
        "ntuser_post_thread_message_caller": f"0x{post_thread_caller:08X}",
        "ntuser_post_thread_message_thread_id": f"0x{post_thread_id:08X}",
        "ntuser_post_thread_message_message": f"0x{post_thread_message:08X}",
        "ntuser_post_thread_message_wparam": f"0x{post_thread_wparam:08X}",
        "ntuser_post_thread_message_lparam": f"0x{post_thread_lparam:08X}",
    }


def parse_patch(value: str) -> tuple[int, bytes]:
    try:
        address_text, hex_text = value.split(":", 1)
        address = int(address_text, 0)
        data = bytes.fromhex(hex_text)
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError(
            "patch must have the form ADDRESS:HEX, for example 0x00A4826B:00000000"
        ) from exc
    if not data:
        raise argparse.ArgumentTypeError("patch data must not be empty")
    return address, data


def parse_conditional_patch(value: str) -> tuple[int, bytes, bytes]:
    try:
        address_text, expected_text, replacement_text = value.split(":", 2)
        address = int(address_text, 0)
        expected = bytes.fromhex(expected_text)
        replacement = bytes.fromhex(replacement_text)
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError(
            "conditional patch must be ADDRESS:EXPECTED_HEX:REPLACEMENT_HEX"
        ) from exc
    if not expected or len(expected) != len(replacement):
        raise argparse.ArgumentTypeError(
            "expected and replacement data must be non-empty and the same size"
        )
    return address, expected, replacement


def parse_capture_breakpoint(value: str) -> tuple[int, bytes]:
    try:
        address_text, expected_text = value.split(":", 1)
        address = int(address_text, 0)
        expected = bytes.fromhex(expected_text)
    except (ValueError, TypeError) as exc:
        raise argparse.ArgumentTypeError(
            "capture breakpoint must have the form ADDRESS:EXPECTED_BYTE"
        ) from exc
    if len(expected) != 1:
        raise argparse.ArgumentTypeError("capture breakpoint expects exactly one byte")
    return address, expected


def dump_memory_image(handle: int, base: int, size: int, output: Path) -> dict[str, Any]:
    page_size = 0x1000
    image = bytearray(size)
    unreadable: list[str] = []
    for offset in range(0, size, page_size):
        requested = min(page_size, size - offset)
        data = read_process(handle, base + offset, requested)
        image[offset : offset + len(data)] = data
        if len(data) != requested:
            unreadable.append(f"0x{offset:08X}")
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_bytes(image)
    return {
        "path": str(output.resolve()),
        "base": f"0x{base:08X}",
        "size": size,
        "sha256": hashlib.sha256(image).hexdigest().upper(),
        "unreadable_page_offsets": unreadable,
    }


def install_packet_opcode_capture(
    process_handle: int, opcode: int
) -> dict[str, Any] | None:
    """Park the first native packet-writer call for one exact opcode.

    The bridge is installed only after the verified writer prologue is visible.
    Non-matching calls execute the original eight-byte prologue and return to
    the untouched writer body.  A matching call publishes its caller context
    and parks before the packet is written, so the diagnostic cannot fabricate
    or duplicate traffic.
    """

    observed = read_process(
        process_handle, CLIENT_PACKET_WRITER, len(CLIENT_PACKET_WRITER_SIGNATURE)
    )
    if observed != CLIENT_PACKET_WRITER_SIGNATURE:
        return None

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote:
        raise win_error("VirtualAllocEx(packet opcode capture)")

    bridge_address = remote
    marker_address = remote + 0x200
    spin_address = remote + 0x300
    bridge = bytearray()

    # Preserve the caller's incoming EFLAGS while checking [esp+8], the
    # packet-writer opcode argument before its original prologue executes.
    bridge += b"\x9C"  # pushfd
    bridge += b"\x81\x7C\x24\x0C" + struct.pack("<I", opcode)
    not_match_displacement = len(bridge) + 2
    bridge += b"\x0F\x85\0\0\0\0"

    # Matching path.  Publish count last so polling never observes a partial
    # register set.  The saved flags are discarded before recording ESP.
    bridge += b"\xA3" + struct.pack("<I", marker_address + 16)  # eax
    bridge += b"\x89\x1D" + struct.pack("<I", marker_address + 20)  # ebx
    bridge += b"\x89\x0D" + struct.pack("<I", marker_address + 24)  # ecx
    bridge += b"\x89\x15" + struct.pack("<I", marker_address + 28)  # edx
    bridge += b"\x89\x35" + struct.pack("<I", marker_address + 32)  # esi
    bridge += b"\x89\x3D" + struct.pack("<I", marker_address + 36)  # edi
    bridge += b"\x89\x2D" + struct.pack("<I", marker_address + 12)  # ebp
    bridge += b"\x8B\x04\x24"  # saved eflags
    bridge += b"\xA3" + struct.pack("<I", marker_address + 40)
    bridge += b"\x83\xC4\x04"  # discard saved eflags
    bridge += b"\x89\x25" + struct.pack("<I", marker_address + 8)  # esp
    bridge += b"\x8B\x04\x24"  # caller return address
    bridge += b"\xA3" + struct.pack("<I", marker_address + 4)
    bridge += b"\xF0\xFF\x05" + struct.pack("<I", marker_address)
    jump_to_spin_offset = len(bridge) + 1
    bridge += b"\xE9\0\0\0\0"

    normal_path = len(bridge)
    bridge += b"\x9D"  # popfd
    bridge += CLIENT_PACKET_WRITER_SIGNATURE
    jump_to_writer_offset = len(bridge) + 1
    bridge += b"\xE9\0\0\0\0"

    struct.pack_into(
        "<i",
        bridge,
        not_match_displacement,
        (bridge_address + normal_path)
        - (bridge_address + not_match_displacement + 4),
    )
    struct.pack_into(
        "<i",
        bridge,
        jump_to_spin_offset,
        spin_address - (bridge_address + jump_to_spin_offset + 4),
    )
    struct.pack_into(
        "<i",
        bridge,
        jump_to_writer_offset,
        (CLIENT_PACKET_WRITER + len(CLIENT_PACKET_WRITER_SIGNATURE))
        - (bridge_address + jump_to_writer_offset + 4),
    )

    write_process(process_handle, bridge_address, bytes(bridge))
    write_process(process_handle, marker_address, b"\0" * 44)
    write_process(process_handle, spin_address, b"\xEB\xFE")
    entry_patch = (
        b"\xE9"
        + struct.pack("<i", bridge_address - (CLIENT_PACKET_WRITER + 5))
        + b"\x90" * (len(CLIENT_PACKET_WRITER_SIGNATURE) - 5)
    )
    written = write_process_protected(
        process_handle, CLIENT_PACKET_WRITER, entry_patch
    )
    if not kernel32.FlushInstructionCache(process_handle, bridge_address, len(bridge)):
        raise win_error("FlushInstructionCache(packet opcode capture bridge)")
    if not kernel32.FlushInstructionCache(
        process_handle, CLIENT_PACKET_WRITER, len(entry_patch)
    ):
        raise win_error("FlushInstructionCache(packet opcode capture entry)")

    return {
        "status": "installed_waiting_for_opcode",
        "opcode": f"0x{opcode:02X}",
        "packet_writer": f"0x{CLIENT_PACKET_WRITER:08X}",
        "observed_prologue": observed.hex(" ").upper(),
        "entry_patch": entry_patch.hex(" ").upper(),
        "entry_bytes_written": written,
        "bridge": f"0x{bridge_address:08X}",
        "bridge_size": len(bridge),
        "marker": f"0x{marker_address:08X}",
        "park_address": f"0x{spin_address:08X}",
        "matching_packet_written": False,
        "direct_packet_writer_used": False,
        "volatile_memory_only": True,
        "executable_modified": False,
    }


def update_packet_opcode_capture(
    process_handle: int, capture: dict[str, Any], elapsed: float
) -> None:
    if capture.get("status") != "installed_waiting_for_opcode":
        return
    marker_address = int(str(capture["marker"]), 16)
    raw = read_process(process_handle, marker_address, 44)
    if len(raw) != 44:
        return
    (
        count,
        return_address,
        esp,
        ebp,
        eax,
        ebx,
        ecx,
        edx,
        esi,
        edi,
        eflags,
    ) = struct.unpack("<11I", raw)
    if not count:
        return
    capture["status"] = "captured_before_packet_write"
    capture["captured_at_seconds"] = round(elapsed, 6)
    capture["invocation_count"] = count
    capture["return_address"] = f"0x{return_address:08X}"
    capture["call_instruction_candidate"] = f"0x{max(0, return_address - 5):08X}"
    capture["registers"] = {
        "eax": f"0x{eax:08X}",
        "ebx": f"0x{ebx:08X}",
        "ecx": f"0x{ecx:08X}",
        "edx": f"0x{edx:08X}",
        "esi": f"0x{esi:08X}",
        "edi": f"0x{edi:08X}",
        "ebp": f"0x{ebp:08X}",
        "esp": f"0x{esp:08X}",
        "eflags": f"0x{eflags:08X}",
    }
    stack = read_process(process_handle, esp, 0x100)
    capture["stack_window"] = {
        "base": f"0x{esp:08X}",
        "size": len(stack),
        "hex": stack.hex().upper(),
        "dwords": [
            f"0x{item:08X}"
            for item in struct.unpack(
                f"<{len(stack) // 4}I", stack[: len(stack) // 4 * 4]
            )
        ],
    }
    code_base = max(0, return_address - 0x20)
    capture["caller_code_window"] = {
        "base": f"0x{code_base:08X}",
        "hex": read_process(process_handle, code_base, 0x60).hex().upper(),
    }


def sample_thread_contexts(pid: int, process_handle: int) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    for thread_id in thread_ids(pid):
        thread = kernel32.OpenThread(
            THREAD_SUSPEND_RESUME | THREAD_GET_CONTEXT | THREAD_QUERY_INFORMATION,
            False,
            thread_id,
        )
        if not thread:
            results.append({"thread_id": thread_id, "error": "OpenThread failed"})
            continue
        suspended = False
        try:
            previous_count = kernel32.SuspendThread(thread)
            if previous_count == 0xFFFFFFFF:
                results.append({"thread_id": thread_id, "error": "SuspendThread failed"})
                continue
            suspended = True
            context = WOW64_CONTEXT()
            context.ContextFlags = WOW64_CONTEXT_FULL
            if not kernel32.Wow64GetThreadContext(thread, ctypes.byref(context)):
                results.append(
                    {"thread_id": thread_id, "error": "Wow64GetThreadContext failed"}
                )
                continue
            instruction_bytes = read_process(process_handle, int(context.Eip), 32)
            stack_bytes = read_process(process_handle, int(context.Esp), 64)
            results.append(
                {
                    "thread_id": thread_id,
                    "registers": {
                        name.lower(): f"0x{int(getattr(context, name)):08X}"
                        for name in (
                            "Eax",
                            "Ebx",
                            "Ecx",
                            "Edx",
                            "Esi",
                            "Edi",
                            "Ebp",
                            "Esp",
                            "Eip",
                            "EFlags",
                        )
                    },
                    "instruction_bytes": instruction_bytes.hex().upper(),
                    "stack_bytes": stack_bytes.hex().upper(),
                }
            )
        finally:
            if suspended:
                kernel32.ResumeThread(thread)
            kernel32.CloseHandle(thread)
    return results


def drain_debug_events(
    pid: int, process_handle: int, report: dict[str, Any]
) -> None:
    """Continue queued debug events and preserve the first second-chance fault."""
    while True:
        event = DEBUG_EVENT()
        if not kernel32.WaitForDebugEvent(ctypes.byref(event), 0):
            error = ctypes.get_last_error()
            if error in (0, 121):  # ERROR_SEM_TIMEOUT
                return
            raise win_error("WaitForDebugEvent")

        continue_status = DBG_CONTINUE
        if event.dwDebugEventCode == EXCEPTION_DEBUG_EVENT:
            info = event.u.Exception
            record = info.ExceptionRecord
            code = int(record.ExceptionCode)
            first_chance = bool(info.dwFirstChance)
            initial_breakpoint = (
                first_chance
                and code == EXCEPTION_BREAKPOINT
                and not report.get("_debug_initial_breakpoint_seen", False)
            )
            if initial_breakpoint:
                report["_debug_initial_breakpoint_seen"] = True
            continue_status = (
                DBG_CONTINUE if initial_breakpoint else DBG_EXCEPTION_NOT_HANDLED
            )
            if not first_chance and "unhandled_debug_exception" not in report:
                contexts = sample_thread_contexts(pid, process_handle)
                faulting_context = next(
                    (
                        row
                        for row in contexts
                        if int(row.get("thread_id", 0)) == int(event.dwThreadId)
                    ),
                    None,
                )
                parameters = [
                    int(record.ExceptionInformation[index])
                    for index in range(min(int(record.NumberParameters), 15))
                ]
                report["unhandled_debug_exception"] = {
                    "code": f"0x{code:08X}",
                    "address": f"0x{int(record.ExceptionAddress or 0):08X}",
                    "thread_id": int(event.dwThreadId),
                    "first_chance": False,
                    "parameters": [f"0x{value:016X}" for value in parameters],
                    "faulting_context": faulting_context,
                }

        if not kernel32.ContinueDebugEvent(
            event.dwProcessId, event.dwThreadId, continue_status
        ):
            raise win_error("ContinueDebugEvent")


def filetime_seconds(value: FILETIME) -> float:
    ticks = (int(value.dwHighDateTime) << 32) | int(value.dwLowDateTime)
    return ticks / 10_000_000.0


def process_metrics(handle: int) -> dict[str, Any]:
    creation = FILETIME()
    exit_time = FILETIME()
    kernel_time = FILETIME()
    user_time = FILETIME()
    io = IO_COUNTERS()
    handles = wt.DWORD()
    memory = PROCESS_MEMORY_COUNTERS_EX()
    memory.cb = ctypes.sizeof(memory)
    result: dict[str, Any] = {}
    if kernel32.GetProcessTimes(
        handle,
        ctypes.byref(creation),
        ctypes.byref(exit_time),
        ctypes.byref(kernel_time),
        ctypes.byref(user_time),
    ):
        result["cpu_kernel_seconds"] = round(filetime_seconds(kernel_time), 6)
        result["cpu_user_seconds"] = round(filetime_seconds(user_time), 6)
        result["cpu_total_seconds"] = round(
            result["cpu_kernel_seconds"] + result["cpu_user_seconds"], 6
        )
    if kernel32.GetProcessIoCounters(handle, ctypes.byref(io)):
        result["io"] = {
            "read_operations": int(io.ReadOperationCount),
            "write_operations": int(io.WriteOperationCount),
            "other_operations": int(io.OtherOperationCount),
            "read_bytes": int(io.ReadTransferCount),
            "write_bytes": int(io.WriteTransferCount),
            "other_bytes": int(io.OtherTransferCount),
        }
    if kernel32.GetProcessHandleCount(handle, ctypes.byref(handles)):
        result["handle_count"] = int(handles.value)
    if psapi.GetProcessMemoryInfo(
        handle, ctypes.byref(memory), ctypes.sizeof(memory)
    ):
        result["memory"] = {
            "working_set_bytes": int(memory.WorkingSetSize),
            "peak_working_set_bytes": int(memory.PeakWorkingSetSize),
            "private_bytes": int(memory.PrivateUsage),
            "page_faults": int(memory.PageFaultCount),
        }
    return result


def create_kill_job() -> int:
    job = kernel32.CreateJobObjectW(None, None)
    if not job:
        raise win_error("CreateJobObjectW")
    limits = JOBOBJECT_EXTENDED_LIMIT_INFORMATION()
    limits.BasicLimitInformation.LimitFlags = JOB_OBJECT_LIMIT_KILL_ON_JOB_CLOSE
    if not kernel32.SetInformationJobObject(
        job,
        JOB_OBJECT_EXTENDED_LIMIT_INFORMATION_CLASS,
        ctypes.byref(limits),
        ctypes.sizeof(limits),
    ):
        kernel32.CloseHandle(job)
        raise win_error("SetInformationJobObject")
    return int(job)


def export_rva(path: Path, name: str) -> int:
    pe = pefile.PE(str(path), fast_load=False)
    for symbol in pe.DIRECTORY_ENTRY_EXPORT.symbols:
        if symbol.name and symbol.name.decode("ascii", "replace") == name:
            if symbol.forwarder:
                raise RuntimeError(f"{name} is forwarded to {symbol.forwarder!r}")
            return int(symbol.address)
    raise RuntimeError(f"export not found: {path.name}!{name}")


def discover_wow64_system_modules() -> list[dict[str, Any]]:
    """Read shared 32-bit system-DLL bases from a short-lived hidden helper."""
    system_root = Path(os.environ.get("SystemRoot", r"C:\Windows"))
    cmd = system_root / "SysWOW64" / "cmd.exe"
    helper = subprocess.Popen(
        [str(cmd), "/d", "/c", "ping", "-n", "6", "127.0.0.1", ">nul"],
        stdin=subprocess.DEVNULL,
        stdout=subprocess.DEVNULL,
        stderr=subprocess.DEVNULL,
        creationflags=CREATE_NO_WINDOW,
    )
    try:
        deadline = time.monotonic() + 4.0
        while time.monotonic() < deadline:
            modules = module_snapshot(helper.pid)
            names = {
                row["name"].casefold()
                for row in modules
                if int(row["base"], 16) <= 0xFFFFFFFF
            }
            if {"ntdll.dll", "kernel32.dll"}.issubset(names):
                return modules
            time.sleep(0.02)
        raise RuntimeError("could not discover WOW64 system module bases")
    finally:
        if helper.poll() is None:
            helper.terminate()
        try:
            helper.wait(timeout=2)
        except subprocess.TimeoutExpired:
            helper.kill()


def install_gap_veh(
    process_handle: int,
    thread_handle: int,
    pid: int,
    scan_instruction: int,
    entry_point: int,
    capture_breakpoint: tuple[int, bytes] | None = None,
    capture_unhandled_av: bool = False,
    shared_resource_gate: bool = False,
    startup_gate_target: int | None = None,
) -> dict[str, Any]:
    modules = module_snapshot(pid)
    source = "target_before_resume"
    if not any(
        row["name"].casefold() == "kernel32.dll"
        and int(row["base"], 16) <= 0xFFFFFFFF
        for row in modules
    ):
        modules = discover_wow64_system_modules()
        source = "hidden_wow64_helper"
    ntdll = next(
        (
            row
            for row in modules
            if row["name"].casefold() == "ntdll.dll"
            and int(row["base"], 16) <= 0xFFFFFFFF
        ),
        None,
    )
    kernel32_module = next(
        (
            row
            for row in modules
            if row["name"].casefold() == "kernel32.dll"
            and int(row["base"], 16) <= 0xFFFFFFFF
        ),
        None,
    )
    if ntdll is None or kernel32_module is None:
        raise RuntimeError("32-bit ntdll/kernel32 modules were not visible before resume")

    system_root = Path(os.environ.get("SystemRoot", r"C:\Windows"))
    rtl_add_veh = int(ntdll["base"], 16) + export_rva(
        system_root / "SysWOW64" / "ntdll.dll",
        "RtlAddVectoredExceptionHandler",
    )
    kernel32_base = int(kernel32_module["base"], 16)
    # SecureEngine scans into the reserved gap between this Windows 10 build's
    # kernel32 .text and .rdata.  The addresses are expressed relative to the
    # live module base so ASLR remains supported.
    fault_eax = kernel32_base + 0x0007B920
    repaired_eax = kernel32_base + 0x0007FFE8

    remote = int(
        kernel32.VirtualAllocEx(
            process_handle,
            None,
            0x1000,
            MEM_COMMIT | MEM_RESERVE,
            PAGE_EXECUTE_READWRITE,
        )
        or 0
    )
    if not remote:
        raise win_error("VirtualAllocEx")
    handler_address = remote
    # Keep generous separation because optional context-capture branches make
    # the handler larger than the original 0x100-byte allocation plan.
    stub_address = remote + 0x400
    result_address = remote + 0x800
    oep_marker_address = remote + 0x804
    context_marker_address = remote + 0x808
    runtime_ready_marker_address = remote + 0x820
    capture_spin_address = remote + 0x900
    access_violation_marker_address = remote + 0xA00
    shared_gate_address = remote + 0xB00
    shared_release_address = remote + 0xB80

    # x86 LONG CALLBACK handler(PEXCEPTION_POINTERS).  Besides repairing the
    # known scan AV, it can record the first single-step EIP inside the original
    # image and clear TF there.  Unrelated exceptions return CONTINUE_SEARCH.
    handler = bytearray()
    labels: dict[str, int] = {}
    near_fixups: list[tuple[int, str]] = []

    def emit(data: bytes) -> None:
        handler.extend(data)

    def label(name: str) -> None:
        labels[name] = len(handler)

    def jump_short(opcode: int, target: str) -> None:
        # Use rel32 encodings even though the helper kept its historical name;
        # context-capture support makes the handler too large for rel8 jumps.
        if opcode == 0xEB:
            emit(b"\xE9\0\0\0\0")
        elif 0x70 <= opcode <= 0x7F:
            emit(bytes((0x0F, opcode + 0x10)) + b"\0\0\0\0")
        else:
            raise ValueError(f"unsupported jump opcode: 0x{opcode:02X}")
        near_fixups.append((len(handler) - 4, target))

    emit(b"\x8B\x44\x24\x04")  # mov eax,[esp+4]
    emit(b"\x8B\x10")  # mov edx,[eax]
    emit(b"\x81\x3A" + struct.pack("<I", 0x80000004))
    jump_short(0x74, "single_step")
    if capture_breakpoint is not None:
        capture_address, capture_original = capture_breakpoint
        emit(b"\x81\x3A" + struct.pack("<I", 0x80000003))
        jump_short(0x74, "capture_breakpoint")
    emit(b"\x81\x3A" + struct.pack("<I", 0xC0000005))
    jump_short(0x75, "search")
    emit(b"\x81\x7A\x0C" + struct.pack("<I", scan_instruction))
    jump_short(0x75, "capture_unhandled_av" if capture_unhandled_av else "search")
    emit(b"\x8B\x50\x04")  # mov edx,[eax+4]
    emit(b"\x81\xBA\xB0\x00\x00\x00" + struct.pack("<I", fault_eax))
    jump_short(0x75, "capture_unhandled_av" if capture_unhandled_av else "search")
    emit(b"\xC7\x82\xB0\x00\x00\x00" + struct.pack("<I", repaired_eax))
    jump_short(0xEB, "handled")

    if capture_unhandled_av:
        label("capture_unhandled_av")
        # The protector deliberately raises access violations before handing
        # control to the unpacked image.  Let those continue through its own
        # exception chain; only park faults after the OEP marker has been set.
        emit(
            b"\x83\x3D"
            + struct.pack("<I", runtime_ready_marker_address)
            + b"\x00"
        )
        jump_short(0x74, "search")
        # Preserve the first otherwise-unhandled access violation and park its
        # faulting thread.  Detailed diagnostics can then read an exact context
        # before the process is terminated at its configured timeout.
        emit(b"\x8B\x10")  # mov edx,[eax] (EXCEPTION_RECORD)
        emit(b"\xF0\xFF\x05" + struct.pack("<I", access_violation_marker_address))
        emit(b"\x8B\x4A\x0C")  # ExceptionAddress
        emit(b"\x89\x0D" + struct.pack("<I", access_violation_marker_address + 4))
        emit(b"\x8B\x4A\x14")  # ExceptionInformation[0]: read/write/execute
        emit(b"\x89\x0D" + struct.pack("<I", access_violation_marker_address + 8))
        emit(b"\x8B\x4A\x18")  # ExceptionInformation[1]: fault address
        emit(b"\x89\x0D" + struct.pack("<I", access_violation_marker_address + 12))
        emit(b"\x8B\x50\x04")  # mov edx,[eax+4] (WOW64_CONTEXT)
        for marker_offset, context_offset in (
            (16, 0xB8),  # EIP
            (20, 0xB0),  # EAX
            (24, 0xA4),  # EBX
            (28, 0xAC),  # ECX
            (32, 0xA8),  # EDX
            (36, 0xA0),  # ESI
            (40, 0x9C),  # EDI
            (44, 0xB4),  # EBP
            (48, 0xC4),  # ESP
        ):
            emit(b"\x8B\x8A" + struct.pack("<I", context_offset))
            emit(b"\x89\x0D" + struct.pack("<I", access_violation_marker_address + marker_offset))
        emit(b"\xC7\x82\xB8\x00\x00\x00" + struct.pack("<I", capture_spin_address))
        jump_short(0xEB, "handled")

    if capture_breakpoint is not None:
        label("capture_breakpoint")
        emit(b"\x81\x7A\x0C" + struct.pack("<I", capture_address))
        jump_short(0x75, "search")
        emit(b"\xC6\x05" + struct.pack("<I", capture_address) + capture_original)
        emit(b"\x8B\x50\x04")  # mov edx,[eax+4]
        # Park the faulting thread after recording its pre-instruction context.
        # This keeps a requested memory dump at the exact capture boundary
        # instead of racing the target while it executes startup code.
        emit(
            b"\xC7\x82\xB8\x00\x00\x00"
            + struct.pack("<I", capture_spin_address)
        )
        emit(b"\xB9" + struct.pack("<I", capture_address))
        jump_short(0xEB, "capture_context")

    label("single_step")
    emit(b"\x8B\x50\x04")  # mov edx,[eax+4]
    emit(b"\x8B\x8A\xB8\x00\x00\x00")  # mov ecx,[edx+0xb8]
    emit(b"\x81\xF9" + struct.pack("<I", 0x00401000))
    jump_short(0x72, "handled")
    emit(b"\x81\xF9" + struct.pack("<I", 0x00837000))
    jump_short(0x73, "handled")
    if startup_gate_target is not None:
        # Only our armed startup breakpoint may enter this gate.
        emit(b"\x81\xF9" + struct.pack("<I", startup_gate_target))
        jump_short(0x75, "search")
    if shared_resource_gate:
        # Redirect only the first real-code single step, never an AV capture.
        emit(b"\xC7\x82\xB8\x00\x00\x00" + struct.pack("<I", shared_gate_address))
    label("capture_context")
    emit(b"\x89\x0D" + struct.pack("<I", oep_marker_address))
    emit(b"\x8B\x8A\xB4\x00\x00\x00")  # mov ecx,[edx+0xb4] (EBP)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address))
    emit(b"\x8B\x8A\xC4\x00\x00\x00")  # mov ecx,[edx+0xc4] (ESP)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address + 4))
    emit(b"\x8B\x8A\xC0\x00\x00\x00")  # mov ecx,[edx+0xc0] (EFlags)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address + 8))
    emit(b"\x8B\x4A\x14")  # mov ecx,[edx+0x14] (Dr6)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address + 12))
    emit(b"\x8B\x4A\x18")  # mov ecx,[edx+0x18] (Dr7)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address + 16))
    emit(b"\x8B\x8A\xB0\x00\x00\x00")  # mov ecx,[edx+0xb0] (EAX)
    emit(b"\x89\x0D" + struct.pack("<I", context_marker_address + 20))
    emit(b"\xC7\x42\x04\x00\x00\x00\x00")  # clear Dr0
    emit(b"\xC7\x42\x14\x00\x00\x00\x00")  # clear Dr6
    emit(b"\xC7\x42\x18\x00\x00\x00\x00")  # clear Dr7
    emit(b"\x81\xA2\xC0\x00\x00\x00" + struct.pack("<I", 0xFFFFFEFF))

    label("handled")
    emit(b"\xB8\xFF\xFF\xFF\xFF\xC2\x04\x00")
    label("search")
    emit(b"\x31\xC0\xC2\x04\x00")
    for displacement_offset, target in near_fixups:
        displacement = labels[target] - (displacement_offset + 4)
        struct.pack_into("<i", handler, displacement_offset, displacement)

    # The entry trampoline runs after loader initialization and before the
    # protected entry point, registers the VEH, restores the original entry
    # bytes, executes them, and jumps back to entry+5.
    original_entry = read_process(process_handle, entry_point, 5)
    if len(original_entry) != 5:
        raise RuntimeError("could not read five protected entry-point bytes")
    stub = bytearray()
    stub += b"\x9C\x60"  # pushfd; pushad
    stub += b"\x68" + struct.pack("<I", handler_address)
    stub += b"\x6A\x01"
    stub += b"\xB8" + struct.pack("<I", rtl_add_veh)
    stub += b"\xFF\xD0"
    stub += b"\xA3" + struct.pack("<I", result_address)
    stub += b"\xC7\x05" + struct.pack("<I", entry_point)
    stub += original_entry[:4]
    stub += b"\xC6\x05" + struct.pack("<I", entry_point + 4)
    stub += original_entry[4:5]
    stub += b"\x61\x9D"  # popad; popfd
    stub += original_entry
    jump_from = stub_address + len(stub) + 5
    stub += b"\xE9" + struct.pack("<i", (entry_point + 5) - jump_from)

    if len(handler) >= 0x400:
        raise RuntimeError("VEH handler overlaps entry trampoline")
    if shared_resource_gate:
        from shared_bootstrap import gate_code, resolve_export
        gate = gate_code(shared_gate_address, shared_release_address, oep_marker_address,
                         resolve_export(modules, "kernel32.dll", "Sleep"),
                         resolve_export(modules, "kernel32.dll", "ExitProcess"))
        if len(gate) >= 0x80:
            raise RuntimeError("shared OEP gate overlaps release marker")
        write_process(process_handle, shared_gate_address, gate)
        write_process(process_handle, shared_release_address, b"\0" * 4)
    write_process(process_handle, handler_address, bytes(handler))
    write_process(process_handle, stub_address, bytes(stub))
    write_process(process_handle, result_address, b"\0\0\0\0")
    write_process(process_handle, oep_marker_address, b"\0\0\0\0")
    write_process(process_handle, context_marker_address, b"\0" * 24)
    write_process(process_handle, runtime_ready_marker_address, b"\0\0\0\0")
    write_process(process_handle, access_violation_marker_address, b"\0" * 52)
    if capture_breakpoint is not None or capture_unhandled_av:
        write_process(process_handle, capture_spin_address, b"\xEB\xFE")
    entry_hook = b"\xE9" + struct.pack("<i", stub_address - (entry_point + 5))
    write_process(process_handle, entry_point, entry_hook)
    result = {
        "remote_base": f"0x{remote:08X}",
        "handler": f"0x{handler_address:08X}",
        "stub": f"0x{stub_address:08X}",
        "result_address": f"0x{result_address:08X}",
        "oep_marker_address": f"0x{oep_marker_address:08X}",
        "context_marker_address": f"0x{context_marker_address:08X}",
        "runtime_ready_marker_address": f"0x{runtime_ready_marker_address:08X}",
        "shared_gate_address": f"0x{shared_gate_address:08X}" if shared_resource_gate else None,
        "shared_release_address": f"0x{shared_release_address:08X}" if shared_resource_gate else None,
        "access_violation_marker_address": f"0x{access_violation_marker_address:08X}",
        "rtl_add_veh": f"0x{rtl_add_veh:08X}",
        "scan_instruction": f"0x{scan_instruction:08X}",
        "fault_eax": f"0x{fault_eax:08X}",
        "repaired_eax": f"0x{repaired_eax:08X}",
        "handler_size": len(handler),
        "stub_size": len(stub),
        "system_module_source": source,
        "entry_point": f"0x{entry_point:08X}",
        "entry_original": original_entry.hex().upper(),
        "entry_hook": entry_hook.hex().upper(),
        "installation_method": "entry_trampoline",
        "capture_unhandled_av": capture_unhandled_av,
    }
    if capture_breakpoint is not None:
        result["capture_breakpoint"] = {
            "address": f"0x{capture_breakpoint[0]:08X}",
            "original": capture_breakpoint[1].hex().upper(),
            "replacement": "CC",
            "park_address": f"0x{capture_spin_address:08X}",
        }
    return result


def enable_trap_flag(thread_handle: int) -> dict[str, str]:
    previous = kernel32.SuspendThread(thread_handle)
    if previous == 0xFFFFFFFF:
        raise win_error("SuspendThread(primary)")
    try:
        context = WOW64_CONTEXT()
        context.ContextFlags = WOW64_CONTEXT_FULL
        if not kernel32.Wow64GetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64GetThreadContext(primary)")
        before = int(context.EFlags)
        context.EFlags = before | 0x100
        if not kernel32.Wow64SetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64SetThreadContext(primary)")
        return {
            "eip": f"0x{int(context.Eip):08X}",
            "eflags_before": f"0x{before:08X}",
            "eflags_after": f"0x{int(context.EFlags):08X}",
        }
    finally:
        kernel32.ResumeThread(thread_handle)


def patch_and_enable_trap(
    process_handle: int,
    thread_handle: int,
    address: int,
    expected: bytes,
    replacement: bytes,
) -> tuple[int, dict[str, str]] | None:
    previous = kernel32.SuspendThread(thread_handle)
    if previous == 0xFFFFFFFF:
        raise win_error("SuspendThread(primary patch)")
    try:
        current = read_process(process_handle, address, len(expected))
        if current != expected:
            return None
        written = write_process(process_handle, address, replacement)
        context = WOW64_CONTEXT()
        context.ContextFlags = WOW64_CONTEXT_FULL
        if not kernel32.Wow64GetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64GetThreadContext(primary patch)")
        before = int(context.EFlags)
        context.EFlags = before | 0x100
        if not kernel32.Wow64SetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64SetThreadContext(primary patch)")
        return written, {
            "eip": f"0x{int(context.Eip):08X}",
            "eflags_before": f"0x{before:08X}",
            "eflags_after": f"0x{int(context.EFlags):08X}",
        }
    finally:
        kernel32.ResumeThread(thread_handle)


def patch_and_enable_hardware_breakpoint(
    process_handle: int,
    thread_handle: int,
    address: int,
    expected: bytes,
    replacement: bytes,
    breakpoint_address: int,
) -> tuple[int, dict[str, str]] | None:
    previous = kernel32.SuspendThread(thread_handle)
    if previous == 0xFFFFFFFF:
        raise win_error("SuspendThread(primary hardware breakpoint)")
    try:
        current = read_process(process_handle, address, len(expected))
        if current != expected:
            return None
        context = WOW64_CONTEXT()
        context.ContextFlags = WOW64_CONTEXT_FULL | 0x00000010
        if not kernel32.Wow64GetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64GetThreadContext(primary hardware breakpoint)")
        before_dr7 = int(context.Dr7)
        context.Dr0 = breakpoint_address
        context.Dr6 = 0
        context.Dr7 = (before_dr7 & ~0x000F0003) | 0x00000001
        if not kernel32.Wow64SetThreadContext(thread_handle, ctypes.byref(context)):
            raise win_error("Wow64SetThreadContext(primary hardware breakpoint)")
        written = write_process(process_handle, address, replacement)
        return written, {
            "eip": f"0x{int(context.Eip):08X}",
            "dr0": f"0x{int(context.Dr0):08X}",
            "dr7_before": f"0x{before_dr7:08X}",
            "dr7_after": f"0x{int(context.Dr7):08X}",
        }
    finally:
        kernel32.ResumeThread(thread_handle)


def redact_report_arguments(arguments: list[str]) -> list[str]:
    """Mask legacy provider payload fields while preserving the launch shape."""
    redacted = list(arguments)
    for index, value in enumerate(redacted):
        if value.lower() != "/kornet":
            continue
        for payload_index in range(index + 1, min(index + 5, len(redacted))):
            redacted[payload_index] = f"<masked:{len(redacted[payload_index])}>"
    return redacted


def run_probe(args: argparse.Namespace) -> int:
    if args.multi_instance_startup_gate and (not args.install_gap_veh or args.trace_oep
            or args.hardware_breakpoint is not None or args.capture_breakpoint is not None):
        raise ValueError("Multi-instance startup gate needs the VEH and exclusive breakpoint ownership")
    if args.shared_resource_candidate is not None:
        if not args.install_gap_veh or args.hardware_breakpoint is not None or args.trace_oep:
            raise ValueError("common-resource candidate uses the graphics-init gate; enable --install-gap-veh without debug breakpoints")
        if args.capture_breakpoint or args.capture_packet_opcode is not None:
            raise ValueError("common-resource candidate cannot combine with diagnostic parking")
        if args.trace_process_exit and not 0 < args.timeout <= 60:
            raise ValueError("common-resource exit diagnostics require a bounded timeout <=60 seconds")
    executable = args.executable.resolve()
    cwd = args.cwd.resolve()
    if not executable.is_file():
        raise FileNotFoundError(executable)
    if not cwd.is_dir():
        raise NotADirectoryError(cwd)
    if args.trace_oep and args.hardware_breakpoint is not None:
        raise ValueError("--trace-oep and --hardware-breakpoint are mutually exclusive")
    if args.capture_breakpoint and (args.trace_oep or args.hardware_breakpoint is not None):
        raise ValueError(
            "--capture-breakpoint is mutually exclusive with trace/hardware breakpoint modes"
        )
    if args.capture_packet_opcode is not None:
        if not 0 <= args.capture_packet_opcode <= 0xFF:
            raise ValueError("--capture-packet-opcode must be between 0x00 and 0xFF")
        if not args.install_gap_veh:
            raise ValueError("--capture-packet-opcode requires --install-gap-veh")
        if (
            args.capture_breakpoint
            or args.trace_oep
            or args.hardware_breakpoint is not None
        ):
            raise ValueError(
                "--capture-packet-opcode is mutually exclusive with other capture modes"
            )
    server_profile = (
        load_server_profile(args.server_profile, args.allow_non_loopback_redirect)
        if args.server_profile is not None
        else None
    )
    if args.launcher_child_mode:
        if server_profile is None or not server_profile["is_loopback"]:
            raise ValueError(
                "launcher-child mode requires a loopback server profile"
            )
        if args.post_root_exit_wait <= 0:
            raise ValueError(
                "launcher-child mode requires --post-root-exit-wait"
            )
    launcher_credentials = consume_launcher_credentials()
    # Keep only the non-secret account identifier for later UI decoration.
    # The mutable credential buffers are intentionally zeroed after login.
    launcher_account = (
        bytes(launcher_credentials[0]) if launcher_credentials is not None else b""
    )
    game_handoff_credentials = (
        tuple(bytearray(value) for value in launcher_credentials)
        if launcher_credentials is not None
        else None
    )
    launcher_character = (
        bytes(launcher_credentials[2]) if launcher_credentials is not None else b""
    )
    physical_native_login_session = bool(
        args.physical_native_login
        or (args.manual_native_selection and launcher_credentials is None)
    )
    if launcher_credentials is not None and server_profile is None:
        raise ValueError("launcher credential bootstrap requires a server profile")
    if args.physical_native_login:
        if launcher_credentials is None:
            raise ValueError(
                "physical native login requires LITO381_ACCOUNT and LITO381_PASSWORD"
            )
        if not any(
            argument.casefold() == "/ltcharsvr" for argument in args.arguments
        ):
            raise ValueError(
                "physical native login requires the original /ltcharsvr argument"
            )
        if not args.manual_native_selection:
            raise ValueError(
                "physical native login requires --manual-native-selection"
            )
        if args.launcher_child_mode:
            raise ValueError(
                "physical native login is only valid for the verified LiTo.bin"
            )
    if args.invoke_transfer_stone_producer:
        if args.transfer_stone_settle_seconds < 0:
            raise ValueError("transfer-stone settle seconds must be non-negative")
        if server_profile is None or not server_profile["is_loopback"]:
            raise ValueError(
                "native transfer-stone producer requires a loopback server profile"
            )
        if not args.manual_native_selection:
            if launcher_credentials is None:
                raise ValueError(
                    "automatic native transfer-stone producer requires launcher credentials"
                )
            if not launcher_credentials[2]:
                raise ValueError(
                    "automatic native transfer-stone producer requires LITO381_CHARACTER"
                )
    executable_sha256 = None
    if (
        server_profile is not None
        or args.display_mode != "auto"
        or launcher_credentials is not None
        or args.trace_window_close
        or args.trace_process_exit
    ):
        executable_sha256 = hashlib.sha256(executable.read_bytes()).hexdigest().upper()
        expected_sha256 = (
            KNOWN_LAUNCHER_SHA256
            if args.launcher_child_mode
            else KNOWN_PROTECTED_SHA256
        )
        if executable_sha256 != expected_sha256:
            raise ValueError(
                "runtime compatibility work is restricted to the verified original "
                + ("LiTo.exe" if args.launcher_child_mode else "LiTo.bin")
                + " SHA-256"
            )

    command_parts = [str(executable), *args.arguments]
    command = ctypes.create_unicode_buffer(subprocess.list2cmdline(command_parts))
    startup = STARTUPINFOW()
    startup.cb = ctypes.sizeof(startup)
    process = PROCESS_INFORMATION()
    launcher_child_handle = 0
    job = create_kill_job()
    old_shared_gate = os.environ.get("LITO_SHARED_RESOURCE_GATE")
    if args.shared_resource_candidate is not None:
        os.environ["LITO_SHARED_RESOURCE_GATE"] = "1"
    old_compat_layer = os.environ.get("__COMPAT_LAYER")
    if args.compat_layer:
        os.environ["__COMPAT_LAYER"] = args.compat_layer
    previous_error_mode = kernel32.SetErrorMode(
        SEM_FAILCRITICALERRORS
        | (0 if args.allow_wer else SEM_NOGPFAULTERRORBOX)
    )

    report: dict[str, Any] = {
        "schema_version": 1,
        "started_at": datetime.now().astimezone().isoformat(),
        "executable": str(executable),
        "executable_sha256": executable_sha256,
        "cwd": str(cwd),
        "arguments": redact_report_arguments(args.arguments),
        "timeout_seconds": args.timeout,
        "safety": {
            "started_suspended": True,
            "kill_on_job_close": True,
            "debugger_attached": bool(args.debug_exceptions),
            "legacy_drivers_loaded_by_probe": False,
            "connect_redirect_in_memory_only": server_profile is not None,
            "non_loopback_redirect_allowed": bool(args.allow_non_loopback_redirect),
        },
        "status": "starting",
        "processes": [],
        "modules": [],
        "samples": [],
    }
    report["selection_flow"] = {
        "selected": (
            "native_manual" if args.manual_native_selection else "compatibility_automatic"
        ),
        "native_server_and_character_selection": bool(args.manual_native_selection),
    }
    if args.capture_packet_opcode is not None:
        report["packet_opcode_capture"] = {
            "status": "waiting_for_decrypted_packet_writer",
            "opcode": f"0x{args.capture_packet_opcode:02X}",
            "packet_writer": f"0x{CLIENT_PACKET_WRITER:08X}",
            "matching_packet_written": False,
            "direct_packet_writer_used": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    report["display_mode"] = {
        "selected": args.display_mode,
        "original_source": (
            "GetDeviceCaps(PLANES) * GetDeviceCaps(BITSPIXEL); "
            "16 selects DDSCL_NORMAL, other values select exclusive 640x480x16"
        ),
        "fullscreen_flag_address": "0x006476C8",
        "patch_in_memory_only": args.display_mode != "auto",
    }
    if server_profile is not None:
        report["connect_redirect"] = {
            "status": "waiting_for_decrypted_connect_iat",
            "profile": {
                key: value
                for key, value in server_profile.items()
                if key != "packed_ip"
            },
        }
        report["lobby_overlay_null_guard"] = {
            "status": "waiting_for_decrypted_callback",
            "callback": f"0x{CLIENT_LOBBY_OVERLAY_CALLBACK:08X}",
            "executable_modified": False,
        }
        report["lito_surface_precedence"] = {
            "status": (
                "skipped_legacy_compatible_resources"
                if args.skip_surface_precedence
                else "waiting_for_decrypted_resource_loader"
            ),
            "resource_loader": f"0x{CLIENT_RESOURCE_OPEN:08X}",
            "policy": (
                "skip newer SpriteXX entry when LTsurf has the same name; "
                "route the verified base-Surf 7400..7404 aliases to LTsurf; "
                "reject exact SpriteXX 400c.img..405c.img aliases so the native "
                "19x21 plain toolbar is retained; preserve all other native "
                "base-Surf then LTsurf fallbacks"
            ),
            "executable_modified": False,
            "pak_files_modified": False,
        }
        if args.manual_native_selection:
            report["manual_news_ready_bridge"] = {
                "status": (
                    "not_required_for_physical_native_login"
                    if physical_native_login_session
                    else "waiting_for_first_login_connect"
                ),
                "handler": f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
                "native_handler_reinvoked": False,
                "direct_packet_writer_used": False,
                "reason": (
                    "the original login button constructs the native UI root; "
                    "preserve sub_48F500's immediate C_E2 producer"
                    if physical_native_login_session
                    else "defer an early S_E6 until the compatibility UI root exists"
                ),
                "volatile_memory_only": True,
                "executable_modified": False,
            }
        if physical_native_login_session:
            report["physical_server_directory_bridge"] = {
                "status": "waiting_for_stable_ui_runtime",
                "receiver": f"0x{CLIENT_SERVER_DIRECTORY_RECEIVER:08X}",
                "root_global": f"0x{CLIENT_SERVER_DIRECTORY_ROOT:08X}",
                "volatile_memory_only": True,
                "executable_modified": False,
            }
            report["physical_character_page_bridge"] = {
                "status": "waiting_for_stable_ui_runtime",
                "handler": f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
                "page_builder": f"0x{CLIENT_CHARACTER_PAGE_BUILDER:08X}",
                "page_resource": "0x1D3A",
                "page_index": 7,
                "expected_character_server_mode": (
                    1 if args.physical_native_login else 0
                ),
                "post_directory_source_a_required": bool(
                    physical_native_login_session
                ),
                "allow_source_b_handoff": not args.physical_native_login,
                "direct_packet_writer_used": False,
                "volatile_memory_only": True,
                "executable_modified": False,
            }
    if launcher_credentials is not None:
        report["credential_bootstrap"] = {
            "status": (
                "reserved_for_physical_native_login_connect_rearm"
                if args.physical_native_login
                else "waiting_for_native_credential_containers"
            ),
            "account_length_bytes": len(launcher_credentials[0]),
            "password_length_bytes": len(launcher_credentials[1]),
            "character_name_supplied": bool(launcher_credentials[2]),
            "credentials_logged": False,
            "initial_auto_submit": not args.physical_native_login,
            "connect_time_rearm": True,
            "executable_modified": False,
        }
    if args.invoke_transfer_stone_producer:
        report["transfer_stone_native_producer"] = {
            "status": "waiting_for_native_world",
            "object_id": f"0x{TRANSFER_STONE_PROBE_OBJECT_ID:08X}",
            "inventory_lookup": f"0x{CLIENT_INVENTORY_LOOKUP:08X}",
            "producer": f"0x{CLIENT_DEFAULT_ITEM_USE:08X}",
            "response_policy": (
                "after C_USE_ITEM, use the normal encrypted socket S_FB path and "
                "the original group-2 selector"
                if args.manual_native_selection
                else "after C_USE_ITEM, exercise the exact native S_FB receiver "
                "and group-2 selector with the current 127.0.0.2:32100 fixture; "
                "do not claim a historical destination catalogue"
            ),
            "volatile_memory_only": True,
            "executable_modified": False,
            "world_entry": {
                "status": (
                    "manual_native_ui_owns_world_entry"
                    if args.manual_native_selection
                    else "waiting_for_native_channel"
                ),
                "scope": (
                    "physical native login, character and room controls"
                    if args.manual_native_selection
                    else "ordinary loopback room prerequisite only"
                ),
                "rental_flow_claimed": False,
                "room_id": TRANSFER_STONE_PROBE_ROOM_ID,
            },
            "inventory_receiver": {
                "status": (
                    "manual_native_socket_inventory_owned"
                    if args.manual_native_selection
                    else "waiting_for_native_world_root"
                ),
                "receiver": f"0x{CLIENT_INVENTORY_BATCH_RECEIVER:08X}",
                "socket_dispatcher_claimed": bool(args.manual_native_selection),
            },
            "directory_receiver": {
                "status": "waiting_for_transfer_stone_use",
                "receiver": f"0x{CLIENT_SERVER_DIRECTORY_RECEIVER:08X}",
                "historical_catalogue_claimed": False,
                "socket_dispatcher_claimed": False,
            },
            "server_click": {
                "status": "waiting_for_native_directory",
                "handler": f"0x{CLIENT_SERVER_DIRECTORY_CLICK:08X}",
                "user_click_claimed": False,
            },
        }
    if args.invoke_rental_town_room_selector:
        report["rental_town_native_room_selector"] = {
            "status": "waiting_for_socket_directory",
            "handler": f"0x{CLIENT_ROOM_DIRECTORY_CLICK:08X}",
            "directory_source": "normal encrypted socket dispatch",
            "expected_room_count": 1,
            "user_click_claimed": False,
            "historical_catalogue_claimed": False,
            "volatile_memory_only": True,
            "executable_modified": False,
        }
    if args.trace_window_close:
        report["window_close_trace"] = {
            "status": "waiting_for_decrypted_destroy_window_iat",
            "iat_slot": f"0x{DESTROY_WINDOW_IAT_SLOT:08X}",
            "diagnostic_only": True,
        }
    if args.trace_process_exit:
        report["process_exit_trace"] = {
            "status": "waiting_for_wow64_ntdll",
            "diagnostic_only": True,
        }
        report["unhandled_exception_trace"] = {
            "status": "waiting_for_wow64_ntdll",
            "diagnostic_only": True,
        }
    seen_pids: set[int] = set()
    seen_modules: set[tuple[int, str, str]] = set()
    try:
        ok = kernel32.CreateProcessW(
            str(executable),
            command,
            None,
            None,
            False,
            CREATE_SUSPENDED
            | CREATE_UNICODE_ENVIRONMENT
            | (DEBUG_ONLY_THIS_PROCESS if args.debug_exceptions else 0),
            None,
            str(cwd),
            ctypes.byref(startup),
            ctypes.byref(process),
        )
        if not ok:
            raise win_error("CreateProcessW")
        root_pid = int(process.dwProcessId)
        report["root_pid"] = root_pid
        if not kernel32.AssignProcessToJobObject(job, process.hProcess):
            raise win_error("AssignProcessToJobObject")
        if args.install_gap_veh:
            report["gap_veh"] = install_gap_veh(
                process.hProcess,
                process.hThread,
                root_pid,
                args.veh_scan_instruction,
                args.veh_entry_point,
                args.capture_breakpoint,
                bool(
                    args.trace_process_exit
                    or args.invoke_rental_town_room_selector
                ),
                shared_resource_gate=args.multi_instance_startup_gate,
                startup_gate_target=0x00480A60 if args.multi_instance_startup_gate else None,
            )
        if args.multi_instance_startup_gate:
            # Arm while CreateProcess's primary thread is still suspended.
            # Polling decrypted bytes races WinMain's singleton checks.
            context = WOW64_CONTEXT()
            context.ContextFlags = WOW64_CONTEXT_FULL | 0x10
            if not kernel32.Wow64GetThreadContext(process.hThread, ctypes.byref(context)):
                raise win_error("Wow64GetThreadContext(startup gate)")
            if context.Dr7 & 0xFF:
                raise RuntimeError("Startup gate refuses occupied debug registers")
            context.Dr0 = 0x00480A60
            context.Dr6 = 0
            context.Dr7 = 1
            if not kernel32.Wow64SetThreadContext(process.hThread, ctypes.byref(context)):
                raise win_error("Wow64SetThreadContext(startup gate)")
            report["startup_gate"] = {"target": "0x00480A60", "armed_before_resume": True,
                                      "status": "waiting"}
        if args.affinity_mask is not None:
            if not kernel32.SetProcessAffinityMask(process.hProcess, args.affinity_mask):
                raise win_error("SetProcessAffinityMask")
            report["affinity_mask"] = f"0x{args.affinity_mask:X}"
        if args.compat_layer:
            report["compat_layer"] = args.compat_layer
        if kernel32.ResumeThread(process.hThread) == 0xFFFFFFFF:
            raise win_error("ResumeThread")
        report["status"] = "running"

        started = time.monotonic()
        next_sample = 0.0
        next_context_sample = 0.0
        next_connect_redirect_probe = 0.0
        next_lobby_overlay_guard_probe = 0.0
        next_lito_surface_precedence_probe = 0.0
        next_credential_bootstrap_probe = 0.1
        game_handoff_credentials_rearmed = False
        next_window_close_trace_probe = 0.0
        next_process_exit_trace_probe = 0.0
        next_unhandled_exception_trace_probe = 0.0
        image_dumped = False
        patches_applied = False
        pending_conditional_patches = list(args.conditional_patch or [])
        display_patch = DISPLAY_MODE_PATCHES.get(args.display_mode)
        if display_patch is not None:
            pending_conditional_patches.append(display_patch)
        if legacy_vs_token_patch_required(
            launcher_credentials_supplied=launcher_credentials is not None,
            physical_native_login_session=physical_native_login_session,
        ):
            pending_conditional_patches.append(CLIENT_LEGACY_VS_TOKEN_CALL)
            legacy_vs_token_report = {
                "address": f"0x{CLIENT_LEGACY_VS_TOKEN_CALL[0]:08X}",
                "patch_in_memory_only": True,
                "reason": (
                    "standalone launch supplies or physically captures the authenticated "
                    "C_LOGIN handoff without the official launcher's legacy VS token state"
                ),
            }
            report["legacy_vs_token_bridge"] = legacy_vs_token_report
            if launcher_credentials is not None:
                report["credential_bootstrap"]["legacy_vs_token_call"] = (
                    legacy_vs_token_report
                )
        if physical_native_login_session and not args.physical_native_login:
            pending_conditional_patches.append(
                CLIENT_STANDALONE_SOURCE_CLEANUP_CALL
            )
            report["standalone_source_cleanup_bridge"] = {
                "address": (
                    f"0x{CLIENT_STANDALONE_SOURCE_CLEANUP_CALL[0]:08X}"
                ),
                "status": "pending",
                "reason": (
                    "the standalone launcher has no classic UI root at "
                    "dword_64AD0C; retain the native channel-page builders "
                    "while skipping only sub_469CA0's obsolete teardown"
                ),
                "volatile_memory_only": True,
                "executable_modified": False,
            }
        if args.capture_breakpoint:
            capture_address, capture_expected = args.capture_breakpoint
            pending_conditional_patches.append(
                (capture_address, capture_expected, b"\xCC")
            )
        veh_result_checked = False
        vs_lobby_ui_rearmed = False
        manual_native_screen_baseline: int | None = None
        room_phase_seen_at: float | None = None
        room_identity_staged = False
        room_bootstrap_record_staged = False
        selection_bridge_active: dict[str, Any] | None = None
        cached_lito_profile: dict[str, Any] | None = None
        cached_selected_character_record: bytes | None = None
        cached_selected_character_index: int | None = None
        next_transfer_stone_producer_attempt = 0.0
        transfer_stone_producer_attempts = 0
        transfer_stone_capture_at: float | None = None
        transfer_probe_channel_seen_at: float | None = None
        transfer_probe_room_seen_at: float | None = None
        transfer_probe_join_invoked = False
        transfer_probe_ready_invoked = False
        transfer_probe_inventory_receiver_invoked = False
        transfer_probe_directory_receiver_invoked = False
        transfer_probe_directory_receiver_at: float | None = None
        transfer_stone_directory_call_baseline: int | None = None
        transfer_probe_server_click_invoked = False
        transfer_probe_server_click_at: float | None = None
        transfer_probe_character_page_baseline: int | None = None
        transfer_probe_battle_character_page_at: float | None = None
        transfer_probe_battle_world_at: float | None = None
        rental_town_room_click_invoked = False
        next_rental_town_room_click_attempt = 0.0
        exit_code = wt.DWORD(STILL_ACTIVE)
        while True:
            elapsed = time.monotonic() - started
            if args.trace_process_exit and args.shared_resource_candidate is None and args.install_gap_veh and elapsed > 1:
                write_process(process.hProcess, int(report["gap_veh"]["runtime_ready_marker_address"],16), struct.pack("<I",1))
            if args.shared_resource_candidate is not None and "shared_resources" not in report:
                from shared_bootstrap import RemoteMemory, install, resolve_export
                import sys
                if elapsed > 10:
                    raise RuntimeError("common-resource graphics-init gate was not reached within 10 seconds")
                modules = module_snapshot(root_pid)
                candidate_ddraw = next((m for m in modules if m["name"].casefold() == "litoddraw.dll"
                    and Path(m["path"]).parent.resolve() == cwd), None)
                if candidate_ddraw is not None:
                    gate = resolve_export([candidate_ddraw], "litoddraw.dll", "LiToSharedResourceGate")
                    gate_bytes = read_process(process.hProcess, gate, 16)
                    if len(gate_bytes) != 16:
                        # A loader snapshot can expose a DLL before its data
                        # pages are readable. Retry within the existing budget.
                        state = wt.DWORD()
                        kernel32.GetExitCodeProcess(process.hProcess, ctypes.byref(state))
                        report["shared_gate_pending"] = {"address": hex(gate), "bytes": len(gate_bytes),
                            "module": candidate_ddraw, "exit_code": state.value}
                        if state.value != STILL_ACTIVE:
                            raise RuntimeError("Native exited while loading resource gate: " + str(state.value))
                        time.sleep(.05)
                        continue
                    gate_values = struct.unpack("<4I", gate_bytes)
                    if gate_values[0] != 0x3152534C:
                        raise RuntimeError("common-resource graphics gate signature mismatch")
                    if gate_values[1] == 1:
                        install_started = time.monotonic()
                        report["shared_resources"] = install(
                            RemoteMemory(sys.modules[__name__], process.hProcess, root_pid),
                            args.shared_resource_candidate.resolve(), trace_resources=args.trace_process_exit)
                        if time.monotonic() - install_started > 20:
                            raise RuntimeError("common-resource installation exceeded gate budget")
                        if write_process(process.hProcess, gate + 4, struct.pack("<I", 2)) != 4:
                            raise RuntimeError("could not release common-resource graphics gate")
                        report["shared_resources"].update(gate_released=True,
                            gate_address=hex(gate), gate_thread=gate_values[2],
                            install_seconds=round(time.monotonic() - install_started,3))
                        write_process(process.hProcess, int(report["gap_veh"]["runtime_ready_marker_address"],16), struct.pack("<I",1))
                    elif gate_values[1] == 3:
                        raise RuntimeError("common-resource graphics gate timed out")
            if args.debug_exceptions:
                drain_debug_events(root_pid, process.hProcess, report)
            if (
                args.trace_process_exit
                and report["unhandled_exception_trace"]["status"]
                == "waiting_for_wow64_ntdll"
                and elapsed >= next_unhandled_exception_trace_probe
            ):
                installed_unhandled_trace = install_unhandled_exception_trace(
                    process.hProcess, root_pid
                )
                if installed_unhandled_trace is not None:
                    installed_unhandled_trace["installed_at_seconds"] = round(
                        elapsed, 6
                    )
                    report["unhandled_exception_trace"] = installed_unhandled_trace
                else:
                    next_unhandled_exception_trace_probe = elapsed + 0.01
            if report.get("unhandled_exception_trace", {}).get("status") in (
                "installed",
                "unhandled_exception_intercepted",
            ):
                update_unhandled_exception_trace(
                    process.hProcess, report["unhandled_exception_trace"]
                )
            if (
                args.trace_process_exit
                and report["process_exit_trace"]["status"]
                == "waiting_for_wow64_ntdll"
                and elapsed >= next_process_exit_trace_probe
            ):
                installed_exit_trace = install_process_exit_trace(
                    process.hProcess, root_pid
                )
                if installed_exit_trace is not None:
                    installed_exit_trace["installed_at_seconds"] = round(
                        elapsed, 6
                    )
                    report["process_exit_trace"] = installed_exit_trace
                else:
                    next_process_exit_trace_probe = elapsed + 0.01
            if report.get("process_exit_trace", {}).get("status") in (
                "installed",
                "exit_intercepted",
            ):
                update_process_exit_trace(
                    process.hProcess, report["process_exit_trace"]
                )
            if (
                args.trace_window_close
                and report["window_close_trace"]["status"]
                == "waiting_for_decrypted_destroy_window_iat"
                and elapsed >= next_window_close_trace_probe
                and (
                    (
                        report.get("connect_redirect", {}).get("status")
                        == "installed"
                        and int(
                            report["connect_redirect"]
                            .get("telemetry", {})
                            .get("call_count", 0)
                        )
                        > 0
                    )
                    or (server_profile is None and elapsed >= 15.0)
                )
            ):
                installed_close_trace = install_window_close_trace(
                    process.hProcess, root_pid
                )
                if installed_close_trace is not None:
                    installed_close_trace["installed_at_seconds"] = round(elapsed, 6)
                    report["window_close_trace"] = installed_close_trace
                else:
                    next_window_close_trace_probe = elapsed + 0.01
            if report.get("window_close_trace", {}).get("status") == "installed":
                update_window_close_trace(
                    process.hProcess, report["window_close_trace"]
                )
            source_session_address=report.get('connect_redirect',{}).get('native_source_session_address',0)
            if source_session_address and server_profile is not None:
                selection=read_process(process.hProcess,source_session_address+12,12)
                if len(selection)==12:
                    source_id,selected_port,generation=struct.unpack('<III',selection)
                    if report.get('native_source_session',{}).get('generation')!=generation:
                        sources=server_profile['native_sources']
                        chosen=next((row for row in sources if row['id']==source_id),None)
                        if source_id and (chosen is None or chosen['port']!=selected_port):
                            raise RuntimeError('Native source selection outside configured endpoints')
                        active=[chosen] if chosen else sources
                        server_profile['classic_source_names']=[row['name'] for row in active]
                        server_profile['dynamic_databases']=[row['database'] for row in active]
                        if chosen:
                            server_profile['origin_source_id']='source-'+str(source_id)
                            server_profile['origin_source_name']=chosen['name']
                        report['native_source_session']={'source_id':source_id,'generation':generation,'pid':root_pid,'same_process':True}
            if (
                server_profile is not None
                and report["connect_redirect"]["status"]
                == "waiting_for_decrypted_connect_iat"
                and elapsed >= next_connect_redirect_probe
            ):
                installed_redirect = install_connect_redirect(
                    process.hProcess,
                    root_pid,
                    server_profile,
                    args.manual_native_selection,
                    launcher_credentials,
                    report["connect_redirect"],
                )
                if installed_redirect is not None:
                    installed_redirect["installed_at_seconds"] = round(elapsed, 6)
                    if physical_native_login_session:
                        written = write_process(
                            process.hProcess,
                            CLIENT_AUTO_LOGIN_FLAG,
                            b"\x00\x01",
                        )
                        if written != 2:
                            raise RuntimeError(
                                "could not arm native retained-source selector flags"
                            )
                        source_modes_written = write_process(
                            process.hProcess,
                            CLIENT_CHARACTER_AUTO_SELECT_SOURCE_A,
                            b"\x00\x00",
                        )
                        if source_modes_written != 2:
                            raise RuntimeError(
                                "could not clear native source-mode shortcuts"
                            )
                        installed_redirect["initial_source_selection"] = {
                            "status": "retained_source_selector_armed",
                            "cached_source_flag_address": (
                                f"0x{CLIENT_AUTO_LOGIN_FLAG:08X}"
                            ),
                            "force_selector_flag_address": (
                                f"0x{CLIENT_NATIVE_SOURCE_FORCE_SELECTOR_FLAG:08X}"
                            ),
                            "values": [0, 1],
                            "reason": (
                                "a standalone native-login session has no official launcher "
                                "source cache; sub_48FAA0 consumes 0x75F756 to show the "
                                "retained-source modal instead of silently sending cached C35"
                            ),
                            "volatile_memory_only": True,
                            "executable_modified": False,
                        }
                    report["connect_redirect"] = installed_redirect
                    if (
                        args.install_gap_veh
                        and (
                            args.trace_process_exit
                            or args.invoke_rental_town_room_selector
                        )
                    ):
                        ready_marker = int(
                            report["gap_veh"]["runtime_ready_marker_address"], 16
                        )
                        write_process(
                            process.hProcess, ready_marker, struct.pack("<I", 1)
                        )
                else:
                    next_connect_redirect_probe = elapsed + 0.01
            if report.get("connect_redirect", {}).get("status") == "installed":
                update_connect_redirect_telemetry(
                    process.hProcess, report["connect_redirect"]
                )
                if physical_native_login_session:
                    source_directory = read_process(
                        process.hProcess, 0x0075F758, 4
                    )
                    source_modal = read_process(
                        process.hProcess, CLIENT_NATIVE_SOURCE_MODAL_ROOT, 4
                    )
                    source_directory_value = (
                        struct.unpack("<I", source_directory)[0]
                        if len(source_directory) == 4
                        else 0
                    )
                    source_modal_value = (
                        struct.unpack("<I", source_modal)[0]
                        if len(source_modal) == 4
                        else 0
                    )
                    if source_directory_value == 0 and source_modal_value == 0:
                        cached_source = read_process(
                            process.hProcess, CLIENT_AUTO_LOGIN_FLAG, 1
                        )
                        if cached_source == b"\x01":
                            written = write_process(
                                process.hProcess, CLIENT_AUTO_LOGIN_FLAG, b"\x00"
                            )
                            if written != 1:
                                raise RuntimeError(
                                    "could not suppress cached C35 before source list"
                                )
                            source_guard = report["connect_redirect"].setdefault(
                                "physical_source_selection_guard",
                                {
                                    "status": "active_until_source_directory",
                                    "flag_address": f"0x{CLIENT_AUTO_LOGIN_FLAG:08X}",
                                    "clears": 0,
                                    "volatile_memory_only": True,
                                    "executable_modified": False,
                                },
                            )
                            source_guard["clears"] += 1
                            source_guard["last_clear_at_seconds"] = round(elapsed, 6)
                connect_count = int(
                    report["connect_redirect"].get("telemetry", {}).get(
                        "call_count", 0
                    )
                )
                manual_news_bridge = report.get("manual_news_ready_bridge")
                if (
                    isinstance(manual_news_bridge, dict)
                    and manual_news_bridge.get("status")
                    == "waiting_for_first_login_connect"
                    and connect_count >= 1
                ):
                    installed_news_bridge = install_manual_news_ready_bridge(
                        process.hProcess
                    )
                    if installed_news_bridge is not None:
                        installed_news_bridge["installed_at_seconds"] = round(
                            elapsed, 6
                        )
                        installed_news_bridge["connect_call_count"] = connect_count
                        report["manual_news_ready_bridge"] = installed_news_bridge
                physical_directory_bridge = report.get(
                    "physical_server_directory_bridge"
                )
                if (
                    isinstance(physical_directory_bridge, dict)
                    and physical_directory_bridge.get("status")
                    == "waiting_for_stable_ui_runtime"
                    and elapsed >= 5.0
                ):
                    installed_directory_bridge = (
                        install_physical_server_directory_bridge(process.hProcess)
                    )
                    if installed_directory_bridge is not None:
                        installed_directory_bridge["installed_at_seconds"] = round(
                            elapsed, 6
                        )
                        installed_directory_bridge["connect_call_count"] = connect_count
                        report["physical_server_directory_bridge"] = (
                            installed_directory_bridge
                        )
                        physical_directory_bridge = installed_directory_bridge
                if (
                    isinstance(physical_directory_bridge, dict)
                    and physical_directory_bridge.get("status") == "installed"
                ):
                    current_receiver_entry = read_process(
                        process.hProcess,
                        CLIENT_SERVER_DIRECTORY_RECEIVER,
                        5,
                    )
                    physical_directory_bridge["receiver_hook_active"] = (
                        current_receiver_entry
                        == bytes.fromhex(
                            str(physical_directory_bridge["entry_patch"])
                        )
                    )
                    current_dispatcher_entry = read_process(
                        process.hProcess,
                        CLIENT_PACKET_DISPATCHER,
                        5,
                    )
                    physical_directory_bridge["dispatcher_hook_active"] = (
                        current_dispatcher_entry
                        == bytes.fromhex(
                            str(
                                physical_directory_bridge[
                                    "dispatcher_entry_patch"
                                ]
                            )
                        )
                    )
                    current_ui_flush_entry = read_process(
                        process.hProcess,
                        CLIENT_TOURNAMENT_UI_FLUSH,
                        len(CLIENT_TOURNAMENT_UI_FLUSH_SIGNATURE),
                    )
                    physical_directory_bridge["ui_flush_guard_active"] = (
                        current_ui_flush_entry
                        == bytes.fromhex(
                            str(
                                physical_directory_bridge[
                                    "ui_flush_entry_patch"
                                ]
                            )
                        )
                    )
                    for address_key, value_key in (
                        ("call_count_address", "call_count"),
                        ("root_created_address", "root_created"),
                        ("room_count_address", "room_count"),
                        ("server_count_address", "server_count"),
                        (
                            "ui_flush_null_guard_count_address",
                            "ui_flush_null_guard_count",
                        ),
                        (
                            "transition_kind_before_address",
                            "transition_kind_before",
                        ),
                        (
                            "transition_kind_normalized_count_address",
                            "transition_kind_normalized_count",
                        ),
                    ):
                        value_raw = read_process(
                            process.hProcess,
                            int(str(physical_directory_bridge[address_key]), 16),
                            4,
                        )
                        physical_directory_bridge[value_key] = (
                            struct.unpack("<I", value_raw)[0]
                            if len(value_raw) == 4
                            else 0
                        )
                    directory_root_raw = read_process(
                        process.hProcess,
                        int(str(physical_directory_bridge["root_snapshot_address"]), 16),
                        4,
                    )
                    physical_directory_bridge["root_snapshot"] = (
                        f"0x{struct.unpack('<I', directory_root_raw)[0]:08X}"
                        if len(directory_root_raw) == 4
                        else "0x00000000"
                    )
                physical_page_bridge = report.get(
                    "physical_character_page_bridge"
                )
                if (
                    isinstance(physical_page_bridge, dict)
                    and physical_page_bridge.get("status")
                    == "waiting_for_stable_ui_runtime"
                    and elapsed >= 5.0
                ):
                    installed_page_bridge = install_physical_character_page_bridge(
                        process.hProcess,
                        expected_character_server_mode=(
                            1 if args.physical_native_login else 0
                        ),
                        arm_post_directory_source_a=bool(
                            physical_native_login_session
                        ),
                        allow_source_b_handoff=not args.physical_native_login,
                    )
                    if installed_page_bridge is not None:
                        installed_page_bridge["installed_at_seconds"] = round(
                            elapsed, 6
                        )
                        installed_page_bridge["connect_call_count"] = connect_count
                        report["physical_character_page_bridge"] = (
                            installed_page_bridge
                        )
                        session_export=report.get('connect_redirect',{}).get('native_source_session_address',0)
                        if session_export:
                            write_process(process.hProcess,session_export+24,
                                struct.pack('<II',int(installed_page_bridge['marker'],16),
                                    int(installed_page_bridge['expected_character_count_address'],16)))
                if (
                    game_handoff_credentials is not None
                    and not game_handoff_credentials_rearmed
                    and report.get("credential_bootstrap", {}).get("status")
                    == "installed_for_process_lifetime"
                    and connect_count >= 2
                ):
                    rearmed = rearm_credential_bootstrap_for_game_handoff(
                        process.hProcess,
                        game_handoff_credentials[0],
                        game_handoff_credentials[1],
                        game_handoff_credentials[2],
                    )
                    rearmed["connect_call_count"] = connect_count
                    rearmed["at_seconds"] = round(elapsed, 6)
                    report["credential_bootstrap"]["game_handoff"] = rearmed
                    game_handoff_credentials_rearmed = True
                    for secret in game_handoff_credentials:
                        secret[:] = b"\x00" * len(secret)
            if (
                server_profile is not None
                and report["lito_surface_precedence"]["status"]
                == "waiting_for_decrypted_resource_loader"
                and elapsed >= next_lito_surface_precedence_probe
            ):
                installed_surface_precedence = install_lito_surface_precedence(
                    process.hProcess
                )
                if installed_surface_precedence is not None:
                    installed_surface_precedence["installed_at_seconds"] = round(
                        elapsed, 6
                    )
                    report["lito_surface_precedence"] = (
                        installed_surface_precedence
                    )
                else:
                    next_lito_surface_precedence_probe = elapsed + 0.01
            if report.get("lito_surface_precedence", {}).get("status") == "installed":
                update_lito_surface_precedence(
                    process.hProcess, report["lito_surface_precedence"]
                )
            if (
                server_profile is not None
                and report["lobby_overlay_null_guard"]["status"]
                == "waiting_for_decrypted_callback"
                and elapsed >= next_lobby_overlay_guard_probe
            ):
                installed_lobby_guard = install_lobby_overlay_null_guard(
                    process.hProcess,
                    native_arena_transition=args.native_arena_transition,
                    preserve_native_character_selection=(
                        args.manual_native_selection
                    ),
                )
                if installed_lobby_guard is not None:
                    installed_lobby_guard["installed_at_seconds"] = round(
                        elapsed, 6
                    )
                    report["lobby_overlay_null_guard"] = installed_lobby_guard
                else:
                    next_lobby_overlay_guard_probe = elapsed + 0.01
            if (
                launcher_credentials is not None
                and report["credential_bootstrap"]["status"]
                == "waiting_for_native_credential_containers"
                and (
                    report.get("connect_redirect", {}).get("status") == "installed"
                    and int(
                        report["connect_redirect"].get("telemetry", {}).get(
                            "call_count", 0
                        )
                    ) > 0
                )
                and elapsed >= next_credential_bootstrap_probe
            ):
                installed_credentials = install_credential_bootstrap(
                    process.hProcess,
                    launcher_credentials[0],
                    launcher_credentials[1],
                    launcher_credentials[2],
                    report["credential_bootstrap"],
                )
                if installed_credentials is not None:
                    installed_credentials["installed_at_seconds"] = round(elapsed, 6)
                    report["credential_bootstrap"] = installed_credentials
                    for secret in launcher_credentials:
                        secret[:] = b"\x00" * len(secret)
                else:
                    next_credential_bootstrap_probe = elapsed + 0.05
            startup_gate_held = False
            if args.multi_instance_startup_gate and report["startup_gate"]["status"] != "released":
                marker_address = int(report["gap_veh"]["oep_marker_address"], 16)
                startup_gate_held = read_process(process.hProcess, marker_address, 4) == struct.pack("<I", 0x00480A60)
                if startup_gate_held:
                    report["startup_gate"].setdefault("reached_seconds", round(elapsed, 6))
                    report["startup_gate"]["status"] = "held"
                elif elapsed > 10:
                    raise RuntimeError("Multi-instance startup gate was not reached")
            if pending_conditional_patches:
                remaining: list[tuple[int, bytes, bytes]] = []
                for address, expected, replacement in pending_conditional_patches:
                    # The unpacker branch is not a guaranteed WinMain barrier.
                    # Gated multi-instance launches release it independently,
                    # then install all native patches at the startup gate.
                    if args.multi_instance_startup_gate and address != 0x00A4827C and not startup_gate_held:
                        remaining.append((address, expected, replacement))
                        continue
                    if address == 0x00A4827C and remaining and not args.multi_instance_startup_gate:
                        remaining.append((address, expected, replacement))
                        continue
                    current = read_process(process.hProcess, address, len(expected))
                    if current == expected:
                        trap_details = None
                        if args.hardware_breakpoint is not None and "oep" not in report:
                            patched = patch_and_enable_hardware_breakpoint(
                                process.hProcess,
                                process.hThread,
                                address,
                                expected,
                                replacement,
                                args.hardware_breakpoint,
                            )
                            if patched is None:
                                remaining.append((address, expected, replacement))
                                continue
                            written, trap_details = patched
                        elif args.trace_oep:
                            patched = patch_and_enable_trap(
                                process.hProcess,
                                process.hThread,
                                address,
                                expected,
                                replacement,
                            )
                            if patched is None:
                                remaining.append((address, expected, replacement))
                                continue
                            written, trap_details = patched
                        else:
                            written = write_process(
                                process.hProcess, address, replacement
                            )
                        row = {
                            "mode": "conditional",
                            "elapsed_seconds": round(elapsed, 6),
                            "address": f"0x{address:08X}",
                            "original": current.hex().upper(),
                            "replacement": replacement.hex().upper(),
                            "bytes_written": written,
                        }
                        if trap_details:
                            row[
                                "hardware_breakpoint"
                                if args.hardware_breakpoint is not None
                                else "trap_flag"
                            ] = trap_details
                        report.setdefault("runtime_patches", []).append(row)
                        if address == CLIENT_STANDALONE_SOURCE_CLEANUP_CALL[0]:
                            bridge = report.get("standalone_source_cleanup_bridge")
                            if isinstance(bridge, dict):
                                bridge["status"] = "installed"
                                bridge["installed_at_seconds"] = round(elapsed, 6)
                    else:
                        remaining.append((address, expected, replacement))
                pending_conditional_patches = remaining
            if startup_gate_held and not pending_conditional_patches:
                from named_ddraw import install as install_named_ddraw
                report["named_ddraw"] = install_named_ddraw(__import__("sys").modules[__name__], process.hProcess, root_pid, cwd)
                if os.environ.get("LITO_NAMED_DDRAW_SMOKE") == "1":
                    report["status"] = "named-loader-only-verified"
                    break
                # Verify all requested writes while native WinMain is held.
                for address, expected, replacement in args.conditional_patch or []:
                    if read_process(process.hProcess, address, len(replacement)) != replacement:
                        raise RuntimeError(f"Startup gate patch verification failed: {address:#x}")
                release = int(report["gap_veh"]["shared_release_address"], 16)
                if write_process(process.hProcess, release, struct.pack("<I", 1)) != 4:
                    raise RuntimeError("Could not release multi-instance startup gate")
                report["startup_gate"].update(status="released", patches_verified=True,
                    released_seconds=round(elapsed,6), debug_registers_cleared_by_handler=True)
            packet_opcode_capture = report.get("packet_opcode_capture")
            if isinstance(packet_opcode_capture, dict):
                if (
                    packet_opcode_capture.get("status")
                    == "waiting_for_decrypted_packet_writer"
                ):
                    installed_packet_capture = install_packet_opcode_capture(
                        process.hProcess, args.capture_packet_opcode
                    )
                    if installed_packet_capture is not None:
                        installed_packet_capture["installed_at_seconds"] = round(
                            elapsed, 6
                        )
                        report["packet_opcode_capture"] = installed_packet_capture
                        packet_opcode_capture = installed_packet_capture
                update_packet_opcode_capture(
                    process.hProcess, packet_opcode_capture, elapsed
                )
            if args.install_gap_veh and not veh_result_checked and elapsed >= 0.25:
                marker_address = int(report["gap_veh"]["result_address"], 16)
                marker = read_process(process.hProcess, marker_address, 4)
                if len(marker) == 4:
                    value = struct.unpack("<I", marker)[0]
                    report["gap_veh"]["registration_handle"] = f"0x{value:08X}"
                    report["gap_veh"]["registered"] = value != 0
                    veh_result_checked = True
            if (
                (args.trace_process_exit or args.invoke_rental_town_room_selector)
                and args.install_gap_veh
                and "unhandled_access_violation" not in report
            ):
                marker_address = int(
                    report["gap_veh"]["access_violation_marker_address"], 16
                )
                marker = read_process(process.hProcess, marker_address, 52)
                if len(marker) == 52:
                    values = struct.unpack("<13I", marker)
                    (
                        count,
                        exception_address,
                        access_type,
                        fault_address,
                        eip,
                        eax,
                        ebx,
                        ecx,
                        edx,
                        esi,
                        edi,
                        ebp,
                        esp,
                    ) = values
                    if count:
                        access_names = {0: "read", 1: "write", 8: "execute"}
                        report["unhandled_access_violation"] = {
                            "count": count,
                            "exception_address": f"0x{exception_address:08X}",
                            "access_type": access_names.get(
                                access_type, f"unknown-{access_type}"
                            ),
                            "fault_address": f"0x{fault_address:08X}",
                            "registers": {
                                "eip": f"0x{eip:08X}",
                                "eax": f"0x{eax:08X}",
                                "ebx": f"0x{ebx:08X}",
                                "ecx": f"0x{ecx:08X}",
                                "edx": f"0x{edx:08X}",
                                "esi": f"0x{esi:08X}",
                                "edi": f"0x{edi:08X}",
                                "ebp": f"0x{ebp:08X}",
                                "esp": f"0x{esp:08X}",
                            },
                            "instruction_window": {
                                "base": f"0x{max(0, eip - 0x20):08X}",
                                "hex": read_process(
                                    process.hProcess, max(0, eip - 0x20), 0x60
                                ).hex().upper(),
                            },
                            "esp_stack_window": {
                                "base": f"0x{esp:08X}",
                                "hex": read_process(
                                    process.hProcess, esp, 0x200
                                ).hex().upper(),
                            },
                            "ebp_stack_window": {
                                "base": f"0x{max(0, ebp - 0x80):08X}",
                                "hex": read_process(
                                    process.hProcess, max(0, ebp - 0x80), 0x180
                                ).hex().upper(),
                            },
                        }
            if (
                (
                    args.trace_oep
                    or args.hardware_breakpoint is not None
                    or args.capture_breakpoint is not None
                )
                and "oep" not in report
                and args.install_gap_veh
            ):
                oep_marker_address = int(
                    report["gap_veh"]["oep_marker_address"], 16
                )
                marker = read_process(process.hProcess, oep_marker_address, 4)
                if len(marker) == 4:
                    value = struct.unpack("<I", marker)[0]
                    if value:
                        context_marker_address = int(
                            report["gap_veh"]["context_marker_address"], 16
                        )
                        context_bytes = read_process(
                            process.hProcess, context_marker_address, 24
                        )
                        context_values = (
                            struct.unpack("<6I", context_bytes)
                            if len(context_bytes) == 24
                            else (0, 0, 0, 0, 0, 0)
                        )
                        ebp, esp, eflags, dr6, dr7, eax = context_values
                        report["oep"] = {
                            "address": f"0x{value:08X}",
                            "rva": f"0x{value - 0x00400000:08X}",
                            "capture_method": (
                                "in_process_veh_software_breakpoint"
                                if args.capture_breakpoint is not None
                                else (
                                    "in_process_veh_hardware_breakpoint"
                                    if args.hardware_breakpoint is not None
                                    else "in_process_veh_single_step"
                                )
                            ),
                            "context": {
                                "ebp": f"0x{ebp:08X}",
                                "esp": f"0x{esp:08X}",
                                "eflags": f"0x{eflags:08X}",
                                "dr6": f"0x{dr6:08X}",
                                "dr7": f"0x{dr7:08X}",
                                "eax": f"0x{eax:08X}",
                            },
                        }
                        if args.capture_breakpoint is not None:
                            fatal_buffer = read_process(
                                process.hProcess, 0x0064B6E4, 0x800
                            )
                            fatal_text = fatal_buffer.split(b"\0", 1)[0]
                            report["oep"]["fatal_error_buffer"] = {
                                "address": "0x0064B6E4",
                                "hex": fatal_text.hex().upper(),
                                "cp950": fatal_text.decode("cp950", errors="replace"),
                            }
                        if (
                            args.capture_breakpoint is not None
                            and args.dump_image is not None
                            and not image_dumped
                        ):
                            report["memory_dump"] = dump_memory_image(
                                process.hProcess,
                                args.dump_base,
                                args.dump_size,
                                args.dump_image.resolve(),
                            )
                            report["memory_dump"]["trigger"] = "capture_breakpoint"
                            image_dumped = True
                        if ebp >= 0x40:
                            stack_base = ebp - 0x40
                            stack_bytes = read_process(
                                process.hProcess, stack_base, 0xA0
                            )
                            report["oep"]["stack_window"] = {
                                "base": f"0x{stack_base:08X}",
                                "size": len(stack_bytes),
                                "hex": stack_bytes.hex().upper(),
                                "dwords": [
                                    f"0x{item:08X}"
                                    for item in struct.unpack(
                                        f"<{len(stack_bytes) // 4}I",
                                        stack_bytes[: len(stack_bytes) // 4 * 4],
                                    )
                                ],
                            }
                        if esp:
                            esp_stack = read_process(process.hProcess, esp, 0x100)
                            report["oep"]["esp_stack_window"] = {
                                "base": f"0x{esp:08X}",
                                "size": len(esp_stack),
                                "hex": esp_stack.hex().upper(),
                                "dwords": [
                                    f"0x{item:08X}"
                                    for item in struct.unpack(
                                        f"<{len(esp_stack) // 4}I",
                                        esp_stack[: len(esp_stack) // 4 * 4],
                                    )
                                ],
                            }
            if args.patch and not patches_applied and elapsed >= args.patch_at:
                applied_rows: list[dict[str, Any]] = []
                for address, data in args.patch:
                    original = read_process(process.hProcess, address, len(data))
                    written = write_process(process.hProcess, address, data)
                    applied_rows.append(
                        {
                            "elapsed_seconds": round(elapsed, 3),
                            "address": f"0x{address:08X}",
                            "original": original.hex().upper(),
                            "replacement": data.hex().upper(),
                            "bytes_written": written,
                        }
                    )
                report["runtime_patches"] = applied_rows
                patches_applied = True
            if (
                args.dump_image is not None
                and not image_dumped
                and elapsed >= args.dump_at
            ):
                report["memory_dump"] = dump_memory_image(
                    process.hProcess,
                    args.dump_base,
                    args.dump_size,
                    args.dump_image.resolve(),
                )
                image_dumped = True
            if elapsed >= next_sample:
                if report.get("connect_redirect", {}).get("status") == "installed":
                    update_connect_redirect_telemetry(
                        process.hProcess, report["connect_redirect"]
                    )
                table = process_snapshot()
                active_pids = descendants(root_pid, table)
                sample_rows: list[dict[str, Any]] = []
                for pid in sorted(active_pids):
                    row = dict(table.get(pid, {"pid": pid}))
                    row["path"] = process_path(pid)
                    sample_rows.append(row)
                    if pid not in seen_pids:
                        seen_pids.add(pid)
                        report["processes"].append({"first_seen_seconds": round(elapsed, 3), **row})
                    for module in module_snapshot(pid):
                        key = (pid, module["path"].casefold(), module["base"])
                        if key not in seen_modules:
                            seen_modules.add(key)
                            report["modules"].append(
                                {"first_seen_seconds": round(elapsed, 3), "pid": pid, **module}
                            )
                sample: dict[str, Any] = {
                    "elapsed_seconds": round(elapsed, 3),
                    "root_metrics": process_metrics(process.hProcess),
                    "processes": sample_rows,
                }
                fullscreen_flag = read_process(process.hProcess, 0x006476C8, 1)
                product_version = read_process(process.hProcess, 0x00636584, 4)
                build_version = read_process(process.hProcess, 0x005033C4, 4)
                client_mode = read_process(process.hProcess, 0x0064BBD4, 4)
                client_phase = read_process(process.hProcess, 0x0064AF98, 4)
                phase_value = (
                    struct.unpack("<I", client_phase)[0]
                    if len(client_phase) == 4
                    else None
                )
                server_protocol_version = read_process(process.hProcess, 0x0063D54C, 4)
                current_screen = read_process(process.hProcess, 0x0075F6E8, 4)
                server_list = read_process(process.hProcess, 0x0075F758, 4)
                native_source_cached_select = read_process(
                    process.hProcess, CLIENT_AUTO_LOGIN_FLAG, 1
                )
                native_source_force_selector = read_process(
                    process.hProcess, CLIENT_NATIVE_SOURCE_FORCE_SELECTOR_FLAG, 1
                )
                login_server_flag = read_process(process.hProcess, 0x0064AFBD, 1)
                character_server_flag = read_process(process.hProcess, 0x0064AFBC, 1)
                character_auto_select_sent = read_process(process.hProcess, 0x0064AFBE, 1)
                character_server_mode = read_process(
                    process.hProcess, CLIENT_CHARACTER_SERVER_MODE, 1
                )
                special_channel_mode_a = read_process(
                    process.hProcess, CLIENT_VS_SPECIAL_CHANNEL_MODE_A, 1
                )
                special_channel_mode_b = read_process(
                    process.hProcess, CLIENT_VS_SPECIAL_CHANNEL_MODE_B, 1
                )
                auto_enter_channel = read_process(process.hProcess, CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG, 1)
                game_ui_active = read_process(process.hProcess, CLIENT_GAME_UI_ACTIVE_FLAG, 1)
                lito_news_defer = read_process(process.hProcess, CLIENT_LITO_NEWS_DEFER_FLAG, 1)
                world_scene = read_process(process.hProcess, CLIENT_WORLD_SCENE, 4)
                static_scene = read_process(process.hProcess, CLIENT_STATIC_SCENE, 4)
                native_source_handlers = {
                    "key": read_process(
                        process.hProcess, CLIENT_GLOBAL_KEY_HANDLER, 4
                    ),
                    "mouse": read_process(
                        process.hProcess, CLIENT_GLOBAL_MOUSE_HANDLER, 4
                    ),
                    "render": read_process(
                        process.hProcess, CLIENT_GLOBAL_RENDER_HANDLER, 4
                    ),
                }
                native_source_modal_root = read_process(
                    process.hProcess, CLIENT_NATIVE_SOURCE_MODAL_ROOT, 4
                )
                native_source_selected_index = read_process(
                    process.hProcess, CLIENT_NATIVE_SOURCE_SELECTED_INDEX, 4
                )
                native_source_count = read_process(
                    process.hProcess, CLIENT_NATIVE_SOURCE_COUNT, 1
                )
                if (
                    args.manual_native_selection
                    and phase_value == 0
                    and selection_bridge_active is not None
                ):
                    restored_bridge = restore_tournament_selection_bridge(
                        process.hProcess, selection_bridge_active
                    )
                    restored_bridge["at_seconds"] = round(elapsed, 6)
                    report.setdefault(
                        "tournament_native_selection_bridge_lifecycle", []
                    ).append(restored_bridge)
                    selection_bridge_active = None
                    room_identity_staged = False
                    room_bootstrap_record_staged = False
                    cached_lito_profile = None
                    cached_selected_character_record = None
                    cached_selected_character_index = None
                arena_start_globals = {
                    name: read_process(process.hProcess, address, 4)
                    for name, address in CLIENT_ARENA_START_GLOBALS.items()
                }
                ready_world_transition: dict[str, Any] = {}
                guard_state = report.get("lobby_overlay_null_guard", {})
                if guard_state.get("status") == "installed":
                    ready_callback = next(
                        (
                            callback
                            for callback in guard_state.get("callbacks", [])
                            if callback.get("name") == "tournament_ready_state"
                        ),
                        None,
                    )
                    if ready_callback is not None:
                        marker_text = ready_callback.get(
                            "world_transition_marker_address"
                        )
                        root_text = ready_callback.get(
                            "world_transition_root_snapshot_address"
                        )
                        controller_text = ready_callback.get(
                            "world_transition_controller_snapshot_address"
                        )
                        ready_return_text = ready_callback.get(
                            "ready_handler_return_marker_address"
                        )
                        ready_thread_text = ready_callback.get(
                            "ready_handler_thread_id_address"
                        )
                        native_addr_text = ready_callback.get(
                            "native_addr_stage_marker_address"
                        )
                        countdown_text = ready_callback.get(
                            "countdown_stage_marker_address"
                        )
                        if (
                            marker_text
                            and root_text
                            and controller_text
                            and ready_return_text
                            and ready_thread_text
                            and native_addr_text
                            and countdown_text
                        ):
                            marker_raw = read_process(
                                process.hProcess, int(marker_text, 16), 4
                            )
                            root_raw = read_process(
                                process.hProcess, int(root_text, 16), 4
                            )
                            controller_raw = read_process(
                                process.hProcess, int(controller_text, 16), 4
                            )
                            ready_return_raw = read_process(
                                process.hProcess, int(ready_return_text, 16), 4
                            )
                            ready_thread_raw = read_process(
                                process.hProcess, int(ready_thread_text, 16), 4
                            )
                            native_addr_raw = read_process(
                                process.hProcess, int(native_addr_text, 16), 4
                            )
                            countdown_raw = read_process(
                                process.hProcess, int(countdown_text, 16), 4
                            )
                            if (
                                len(marker_raw) == 4
                                and len(root_raw) == 4
                                and len(controller_raw) == 4
                                and len(ready_return_raw) == 4
                                and len(ready_thread_raw) == 4
                                and len(native_addr_raw) == 4
                                and len(countdown_raw) == 4
                            ):
                                ready_world_transition = {
                                    "stage": struct.unpack("<I", marker_raw)[0],
                                    "root": (
                                        f"0x{struct.unpack('<I', root_raw)[0]:08X}"
                                    ),
                                    "controller": (
                                        f"0x{struct.unpack('<I', controller_raw)[0]:08X}"
                                    ),
                                    "ready_handler_returned": struct.unpack(
                                        "<I", ready_return_raw
                                    )[0],
                                    "ready_handler_thread_id": struct.unpack(
                                        "<I", ready_thread_raw
                                    )[0],
                                    "native_addr_stage": struct.unpack(
                                        "<I", native_addr_raw
                                    )[0],
                                    "countdown_stage": struct.unpack(
                                        "<I", countdown_raw
                                    )[0],
                                }
                                ready_callback["world_transition_telemetry"] = dict(
                                    ready_world_transition
                                )
                profile_id_raw = read_process(process.hProcess, CLIENT_LITO_PROFILE_ID, 4)
                profile_name_raw = read_process(
                    process.hProcess,
                    CLIENT_TOURNAMENT_CHARACTER_NAME,
                    CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY,
                )
                profile_id = (
                    struct.unpack("<I", profile_id_raw)[0]
                    if len(profile_id_raw) == 4
                    else 0
                )
                profile_name = profile_name_raw.split(b"\x00", 1)[0]
                if profile_id and profile_name and phase_value in (1, 2, 3, 4):
                    def profile_byte(address: int, fallback: int) -> int:
                        raw = read_process(process.hProcess, address, 1)
                        return raw[0] if len(raw) == 1 else fallback

                    def profile_word(address: int, fallback: int) -> int:
                        raw = read_process(process.hProcess, address, 2)
                        return struct.unpack("<H", raw)[0] if len(raw) == 2 else fallback

                    clan_raw = read_process(
                        process.hProcess,
                        CLIENT_LITO_PROFILE_CLAN,
                        CLIENT_LITO_PROFILE_CLAN_CAPACITY,
                    )
                    level_plus_12 = profile_byte(CLIENT_LITO_PROFILE_LEVEL_PLUS_12, 13)
                    cached_lito_profile = {
                        "id": profile_id,
                        "name": profile_name,
                        "clan": clan_raw.split(b"\x00", 1)[0],
                        "type": profile_byte(CLIENT_LITO_PROFILE_TYPE, 0),
                        "max_hp": profile_word(CLIENT_LITO_PROFILE_MAX_HP, 50),
                        "max_mp": profile_word(CLIENT_LITO_PROFILE_MAX_MP, 10),
                        "armor_class": profile_byte(CLIENT_LITO_PROFILE_ARMOR_CLASS, 10),
                        "level": max(1, level_plus_12 - 12),
                        "strength": profile_byte(CLIENT_LITO_PROFILE_STRENGTH, 13),
                        "constitution": profile_byte(CLIENT_LITO_PROFILE_CONSTITUTION, 10),
                        "dexterity": profile_byte(CLIENT_LITO_PROFILE_DEXTERITY, 10),
                        "charisma": profile_byte(CLIENT_LITO_PROFILE_CHARISMA, 9),
                        "intelligence": profile_byte(CLIENT_LITO_PROFILE_INTELLIGENCE, 8),
                        "wisdom": profile_byte(CLIENT_LITO_PROFILE_WISDOM, 11),
                        "sex": profile_byte(CLIENT_LITO_PROFILE_SEX, 0),
                    }
                    report["lito_character_profile_snapshot"] = {
                        "status": "captured_before_channel_cleanup",
                        "at_seconds": round(elapsed, 6),
                        "object_id": profile_id,
                        "character_name_logged": False,
                    }
                    if not room_bootstrap_record_staged:
                        bootstrap_identity = launcher_character or profile_name
                        record = build_tournament_bootstrap_record(
                            bootstrap_identity,
                            cached_lito_profile,
                        )
                        written = write_process(
                            process.hProcess,
                            CLIENT_TOURNAMENT_BOOTSTRAP_RECORD,
                            record,
                        )
                        if written != len(record):
                            raise RuntimeError(
                                "could not stage the Tournament character bootstrap record"
                            )
                        room_bootstrap_record_staged = True
                        report["tournament_world_bootstrap_record"] = {
                            "status": "staged_before_channel_transition",
                            "source": "captured_s_cb_subtype_0",
                            "at_seconds": round(elapsed, 6),
                            "address": f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_RECORD:08X}",
                            "size": len(record),
                            "character_name_logged": False,
                            "executable_modified": False,
                        }
                        if args.manual_native_selection:
                            selection_bridge = install_tournament_selection_bridge(
                                process.hProcess
                            )
                            selection_bridge_active = selection_bridge
                            selection_bridge["installed_at_seconds"] = round(
                                elapsed,
                                6,
                            )
                            report["tournament_native_selection_bridge"] = (
                                selection_bridge
                            )
                transfer_probe = report.get("transfer_stone_native_producer")
                if isinstance(transfer_probe, dict):
                    world_entry = transfer_probe.get("world_entry")
                    if (
                        isinstance(world_entry, dict)
                        and not args.manual_native_selection
                        and phase_value == 1
                    ):
                        if transfer_probe_channel_seen_at is None:
                            transfer_probe_channel_seen_at = elapsed
                        elif (
                            not transfer_probe_join_invoked
                            and elapsed - transfer_probe_channel_seen_at >= 0.5
                        ):
                            attempt = invoke_transfer_stone_world_entry_producer(
                                process.hProcess,
                                "join",
                            )
                            attempt["at_seconds"] = round(elapsed, 6)
                            world_entry["join"] = attempt
                            world_entry["status"] = "waiting_for_native_room"
                            transfer_probe_join_invoked = True
                    if (
                        isinstance(world_entry, dict)
                        and not args.manual_native_selection
                        and phase_value == 2
                    ):
                        if transfer_probe_room_seen_at is None:
                            transfer_probe_room_seen_at = elapsed
                        elif (
                            not transfer_probe_ready_invoked
                            and elapsed - transfer_probe_room_seen_at >= 0.5
                        ):
                            attempt = invoke_transfer_stone_world_entry_producer(
                                process.hProcess,
                                "ready",
                            )
                            attempt["at_seconds"] = round(elapsed, 6)
                            world_entry["ready"] = attempt
                            world_entry["status"] = "waiting_for_native_world"
                            transfer_probe_ready_invoked = True
                    if isinstance(world_entry, dict) and phase_value == 4:
                        world_entry["status"] = "native_world_reached"
                    if (
                        transfer_probe_battle_character_page_at is not None
                        and transfer_probe_battle_world_at is None
                        and phase_value == 4
                        and ready_world_transition.get("native_addr_stage", 0) >= 3
                        and ready_world_transition.get("countdown_stage", 0) >= 2
                    ):
                        transfer_probe_battle_world_at = elapsed
                        transfer_probe["battle_endpoint_world"] = {
                            "status": "native_rental_battle_world_reached",
                            "at_seconds": round(elapsed, 6),
                            "client_phase": phase_value,
                            "native_addr_stage": ready_world_transition[
                                "native_addr_stage"
                            ],
                            "countdown_stage": ready_world_transition[
                                "countdown_stage"
                            ],
                            "evidence_boundary": (
                                "native destination page reconstruction followed by "
                                "the original READY/C_ADDR/countdown/S_WORLD transition"
                            ),
                            "socket_dispatcher_claimed": True,
                            "executable_modified": False,
                        }
                        if isinstance(world_entry, dict):
                            world_entry["status"] = (
                                "native_rental_battle_world_reached"
                            )
                            world_entry["rental_flow_claimed"] = True
                if (
                    isinstance(transfer_probe, dict)
                    and not args.manual_native_selection
                    and not transfer_probe_inventory_receiver_invoked
                    and phase_value in (3, 4)
                    and struct.unpack("<I", world_scene)[0] != 0
                ):
                    receiver = invoke_transfer_stone_inventory_receiver(
                        process.hProcess
                    )
                    receiver["at_seconds"] = round(elapsed, 6)
                    transfer_probe["inventory_receiver"] = receiver
                    transfer_probe_inventory_receiver_invoked = True
                if (
                    isinstance(transfer_probe, dict)
                    and transfer_probe.get("status")
                    in ("waiting_for_native_world", "waiting_for_inventory_object")
                    and phase_value in (3, 4)
                    and elapsed >= next_transfer_stone_producer_attempt
                ):
                    transfer_stone_producer_attempts += 1
                    attempt = invoke_transfer_stone_native_producer(process.hProcess)
                    attempt["attempt"] = transfer_stone_producer_attempts
                    attempt["at_seconds"] = round(elapsed, 6)
                    attempt["client_phase"] = phase_value
                    attempt["producer_boundary"] = (
                        "direct diagnostic call to original sub_445BD0 after the "
                        "native inventory object exists; phase-4 outer UI click "
                        "handler sub_445C10 is not claimed"
                    )
                    transfer_probe.update(attempt)
                    transfer_probe.setdefault("attempt_history", []).append(
                        attempt.copy()
                    )
                    if attempt.get("status") == "native_producer_invoked":
                        transfer_stone_capture_at = elapsed
                        if args.manual_native_selection:
                            connect_redirect = report.get("connect_redirect")
                            if not isinstance(connect_redirect, dict):
                                raise RuntimeError(
                                    "connect redirect is unavailable for the "
                                    "manual transfer-stone flow"
                                )
                            marker_address = int(
                                str(
                                    connect_redirect[
                                        "rental_transfer_auto_enter_address"
                                    ]
                                ),
                                16,
                            )
                            marker_written = write_process(
                                process.hProcess,
                                marker_address,
                                struct.pack("<I", 1),
                            )
                            if marker_written != 4:
                                raise RuntimeError(
                                    "could not arm rental transfer auto-enter"
                                )
                            transfer_probe["destination_auto_enter"] = {
                                "status": "armed_after_native_transfer_producer",
                                "marker_address": f"0x{marker_address:08X}",
                                "native_flag_address": (
                                    f"0x{CLIENT_VS_AUTO_ENTER_CHANNEL_FLAG:08X}"
                                ),
                                "executable_modified": False,
                            }
                            directory_bridge = report.get(
                                "physical_server_directory_bridge"
                            )
                            if not isinstance(directory_bridge, dict):
                                raise RuntimeError(
                                    "physical S_FB bridge is unavailable for the "
                                    "manual transfer-stone flow"
                                )
                            transfer_stone_directory_call_baseline = int(
                                directory_bridge.get("call_count", 0)
                            )
                    next_transfer_stone_producer_attempt = elapsed + 0.5
                if (
                    isinstance(transfer_probe, dict)
                    and transfer_stone_capture_at is not None
                    and not transfer_probe_directory_receiver_invoked
                    and elapsed - transfer_stone_capture_at >= 0.25
                ):
                    if args.manual_native_selection:
                        directory_bridge = report.get(
                            "physical_server_directory_bridge"
                        )
                        current_call_count = (
                            int(directory_bridge.get("call_count", 0))
                            if isinstance(directory_bridge, dict)
                            else 0
                        )
                        baseline = transfer_stone_directory_call_baseline
                        if baseline is not None and current_call_count > baseline:
                            transfer_probe["directory_receiver"] = {
                                "status": "native_socket_directory_received",
                                "source": "normal encrypted socket dispatch",
                                "call_count_before": baseline,
                                "call_count_after": current_call_count,
                                "historical_catalogue_claimed": False,
                                "socket_dispatcher_claimed": True,
                                "at_seconds": round(elapsed, 6),
                            }
                            transfer_probe_directory_receiver_invoked = True
                    else:
                        directory_receiver = invoke_transfer_stone_directory_receiver(
                            process.hProcess
                        )
                        directory_receiver["at_seconds"] = round(elapsed, 6)
                        transfer_probe["directory_receiver"] = directory_receiver
                        transfer_probe_directory_receiver_invoked = (
                            directory_receiver.get("status")
                            == "native_directory_receiver_invoked"
                        )
                    if transfer_probe_directory_receiver_invoked:
                        transfer_probe_directory_receiver_at = elapsed
                if (
                    isinstance(transfer_probe, dict)
                    and transfer_probe_directory_receiver_at is not None
                    and not transfer_probe_server_click_invoked
                    and elapsed - transfer_probe_directory_receiver_at >= 0.25
                ):
                    server_click = invoke_transfer_stone_server_click(process.hProcess)
                    server_click["at_seconds"] = round(elapsed, 6)
                    transfer_probe["server_click"] = server_click
                    transfer_probe_server_click_invoked = (
                        server_click.get("status") == "native_server_click_invoked"
                    )
                    if transfer_probe_server_click_invoked:
                        transfer_probe_server_click_at = elapsed
                        physical_page_bridge = report.get(
                            "physical_character_page_bridge"
                        )
                        if isinstance(physical_page_bridge, dict):
                            transfer_probe_character_page_baseline = int(
                                physical_page_bridge.get("invocation_count", 0)
                            )
                rental_selector = report.get("rental_town_native_room_selector")
                if (
                    isinstance(rental_selector, dict)
                    and not rental_town_room_click_invoked
                    and phase_value == 1
                    and elapsed >= next_rental_town_room_click_attempt
                ):
                    room_click = invoke_rental_town_room_click(process.hProcess)
                    room_click["at_seconds"] = round(elapsed, 6)
                    rental_selector.update(room_click)
                    attempt_history = rental_selector.setdefault("attempt_history", [])
                    attempt_key = (
                        room_click.get("status"),
                        room_click.get("directory_snapshot", {}).get("status"),
                        room_click.get("directory_snapshot", {}).get("room_count"),
                        room_click.get("directory_snapshot", {}).get("server_count"),
                    )
                    previous_key = rental_selector.get("last_attempt_key")
                    if previous_key != list(attempt_key) or room_click.get(
                        "status"
                    ) == "native_room_click_invoked":
                        attempt_history.append(room_click.copy())
                    rental_selector["last_attempt_key"] = list(attempt_key)
                    rental_town_room_click_invoked = (
                        room_click.get("status") == "native_room_click_invoked"
                    )
                    next_rental_town_room_click_attempt = elapsed + 0.25
                # Phase 1 is assigned slightly before the native C_C9
                # character-selection producer runs.  The create-room object
                # exists only after sub_46BA10 has completed the lobby
                # transition, so use it as the readiness barrier.  Writing
                # the display alias earlier would make C_C9 submit ID(source)
                # instead of the raw database character name.
                create_room_dialog = read_process(
                    process.hProcess, 0x0064A930, 4
                )
                native_lobby_initialized = (
                    len(create_room_dialog) == 4
                    and struct.unpack("<I", create_room_dialog)[0] != 0
                )
                if (
                    not room_identity_staged
                    and phase_value in (1, 2, 3)
                    and native_lobby_initialized
                ):
                    if room_phase_seen_at is None:
                        room_phase_seen_at = elapsed
                    elif elapsed - room_phase_seen_at >= 0.25:
                        current_room_name = read_process(
                            process.hProcess,
                            CLIENT_ROOM_LOCAL_NAME,
                            CLIENT_ROOM_LOCAL_NAME_CAPACITY,
                        )
                        identity = launcher_character
                        if identity:
                            identity_source = "launcher_credentials"
                        elif cached_lito_profile and cached_lito_profile.get("name"):
                            identity = bytes(cached_lito_profile["name"])
                            identity_source = "native_lito_profile"
                        else:
                            selected_character = read_process(
                                process.hProcess,
                                CLIENT_TOURNAMENT_CHARACTER_NAME,
                                CLIENT_TOURNAMENT_CHARACTER_NAME_CAPACITY,
                            )
                            identity = selected_character.split(b"\x00", 1)[0]
                            identity_source = "native_tournament_character"
                        if not identity and current_room_name[:1] != b"\x00":
                            identity = current_room_name.split(b"\x00", 1)[0]
                            identity_source = "native_room_record"
                        if not identity:
                            selected_character = read_process(
                                process.hProcess,
                                CLIENT_CHARACTER_NAME,
                                CLIENT_CHARACTER_NAME_CAPACITY,
                            )
                            identity = selected_character.split(b"\x00", 1)[0]
                            identity_source = "native_lineage_character"
                        selected_index_raw = read_process(
                            process.hProcess,
                            CLIENT_CHARACTER_SELECTED_INDEX,
                            4,
                        )
                        selected_index = (
                            struct.unpack("<I", selected_index_raw)[0]
                            if len(selected_index_raw) == 4
                            else 0xFFFFFFFF
                        )
                        if 0 <= selected_index < 4:
                            selected_record = read_process(
                                process.hProcess,
                                CLIENT_CHARACTER_DIRECTORY_RECORDS
                                + selected_index
                                * CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE,
                                CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE,
                            )
                            selected_record_name = selected_record[:28].split(
                                b"\x00", 1
                            )[0]
                            if (
                                len(selected_record)
                                == CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE
                                and selected_record_name
                                and selected_record_name
                            ):
                                cached_selected_character_record = selected_record
                                cached_selected_character_index = selected_index
                                if not identity:
                                    identity = selected_record_name
                                    identity_source = "native_selected_s_cb_row"
                        display_identity = b""
                        # A physical native login keeps the account only in
                        # the process-private connect bridge.  Recover that
                        # bounded identifier here, after character selection,
                        # so the client can use the same ID(source) identity
                        # that the server publishes in S_CD/S_D1/S_D0.  Never
                        # copy the password out of the target process and never
                        # serialize the account bytes into the launch report.
                        display_account = launcher_account
                        display_account_source = (
                            "launcher_credentials"
                            if launcher_account
                            else ""
                        )
                        connect_redirect = report.get("connect_redirect")
                        if (
                            not display_account
                            and args.manual_native_selection
                            and isinstance(connect_redirect, dict)
                            and connect_redirect.get("status") == "installed"
                        ):
                            credential_valid_address = int(
                                str(connect_redirect["credential_valid_address"]),
                                16,
                            )
                            credential_valid = read_process(
                                process.hProcess,
                                credential_valid_address,
                                4,
                            )
                            if (
                                len(credential_valid) == 4
                                and struct.unpack("<I", credential_valid)[0] == 1
                            ):
                                connect_bridge = int(
                                    str(connect_redirect["hook_address"]), 16
                                )
                                captured_account = read_process(
                                    process.hProcess,
                                    connect_bridge + 0x820,
                                    CLIENT_ACCOUNT_CELL_COUNT,
                                )
                                display_account = captured_account.split(
                                    b"\x00", 1
                                )[0]
                                if display_account:
                                    display_account_source = (
                                        "process_private_native_login_capture"
                                    )
                        source_index = (
                            struct.unpack("<I", native_source_selected_index)[0]
                            if len(native_source_selected_index) == 4 else 0
                        )
                        source_names = (
                            server_profile.get("classic_source_names", [])
                            if server_profile is not None else []
                        )
                        if display_account and source_names:
                            if not 0 <= source_index < len(source_names):
                                source_index = 0
                            display_identity = build_lito_display_identity(
                                display_account,
                                source_names[source_index],
                            )
                        if display_identity:
                            display_wire = display_identity + b"\x00"
                            for display_address in (
                                CLIENT_TOURNAMENT_CHARACTER_NAME,
                                CLIENT_ROOM_LOCAL_NAME,
                            ):
                                written = write_process(
                                    process.hProcess,
                                    display_address,
                                    display_wire,
                                )
                                if written != len(display_wire):
                                    raise RuntimeError(
                                        "could not stage the ID(source) display identity"
                                    )
                            identity = display_identity
                            identity_source = "launcher_account_and_selected_source"
                            current_room_name = display_wire
                            report["display_identity_handoff"] = {
                                "status": "staged_after_character_selection",
                                "source_index": source_index,
                                "source_name": source_names[source_index],
                                "dynamic_database": (
                                    server_profile.get("dynamic_databases", [])[source_index]
                                    if source_index < len(server_profile.get(
                                        "dynamic_databases", []
                                    )) else ""
                                ),
                                "format": "ID(source)",
                                "account_source": display_account_source,
                                "encoded_bytes": len(display_identity),
                                "account_logged": False,
                                "character_name_logged": False,
                                "volatile_memory_only": True,
                                "executable_modified": False,
                            }
                        if current_room_name[:1] == b"\x00" and identity:
                            if len(identity) >= CLIENT_ROOM_LOCAL_NAME_CAPACITY:
                                raise RuntimeError(
                                    "selected character exceeds the Tournament room identity buffer"
                                )
                            room_name_wire = identity + b"\x00"
                            written = write_process(
                                process.hProcess,
                                CLIENT_ROOM_LOCAL_NAME,
                                room_name_wire,
                            )
                            if written != len(room_name_wire):
                                raise RuntimeError(
                                    "could not stage the local Tournament room identity"
                                )
                            status = "staged_after_room_records"
                        elif display_identity:
                            status = "staged_id_source_display_identity"
                        elif current_room_name[:1] != b"\x00":
                            status = "native_room_identity_already_present"
                        else:
                            status = None
                        if status is not None:
                            if (
                                not room_bootstrap_record_staged
                                and identity
                            ):
                                if cached_selected_character_record is not None:
                                    record = clone_tournament_bootstrap_record(
                                        identity,
                                        cached_selected_character_record,
                                    )
                                    record_source = "selected_native_s_cb_row"
                                else:
                                    record = build_tournament_bootstrap_record(
                                        identity,
                                        cached_lito_profile,
                                    )
                                    record_source = (
                                        "captured_s_cb_subtype_0"
                                        if cached_lito_profile
                                        else "safe_local_defaults"
                                    )
                                written = write_process(
                                    process.hProcess,
                                    CLIENT_TOURNAMENT_BOOTSTRAP_RECORD,
                                    record,
                                )
                                if written != len(record):
                                    raise RuntimeError(
                                        "could not stage the Tournament character bootstrap record"
                                    )
                                room_bootstrap_record_staged = True
                                report["tournament_world_bootstrap_record"] = {
                                    "status": "staged_after_room_records_fallback",
                                    "source": record_source,
                                    "selected_index": cached_selected_character_index,
                                    "at_seconds": round(elapsed, 6),
                                    "address": f"0x{CLIENT_TOURNAMENT_BOOTSTRAP_RECORD:08X}",
                                    "size": len(record),
                                    "character_name_logged": False,
                                    "executable_modified": False,
                                }
                                if args.manual_native_selection:
                                    selection_bridge = (
                                        install_tournament_selection_bridge(
                                            process.hProcess
                                        )
                                    )
                                    selection_bridge_active = selection_bridge
                                    selection_bridge["installed_at_seconds"] = round(
                                        elapsed,
                                        6,
                                    )
                                    report["tournament_native_selection_bridge"] = (
                                        selection_bridge
                                    )
                            room_identity_staged = True
                            report["room_identity_handoff"] = {
                                "status": status,
                                "source": identity_source,
                                "at_seconds": round(elapsed, 6),
                                "address": f"0x{CLIENT_ROOM_LOCAL_NAME:08X}",
                                "phase": phase_value,
                                "character_name_logged": False,
                                "executable_modified": False,
                            }
                elif phase_value not in (1, 2, 3):
                    room_phase_seen_at = None
                screen_value = (
                    struct.unpack("<I", current_screen)[0]
                    if len(current_screen) == 4
                    else 0
                )
                server_list_value = (
                    struct.unpack("<I", server_list)[0]
                    if len(server_list) == 4
                    else 0
                )
                mode_value = (
                    struct.unpack("<I", client_mode)[0]
                    if len(client_mode) == 4
                    else None
                )
                character_directory_record = read_process(
                    process.hProcess,
                    CLIENT_CHARACTER_DIRECTORY_RECORDS,
                    CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE,
                )
                directory_name = (
                    character_directory_record.split(b"\x00", 1)[0]
                    if len(character_directory_record)
                    == CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE
                    else b""
                )
                character_directory_index = read_process(
                    process.hProcess, CLIENT_CHARACTER_DIRECTORY_INDEX, 4
                )
                character_directory_pending_count = read_process(
                    process.hProcess,
                    CLIENT_CHARACTER_DIRECTORY_PENDING_COUNT,
                    4,
                )
                current_screen_page = (
                    read_process(process.hProcess, screen_value + 0x0C, 4)
                    if screen_value != 0
                    else b""
                )
                current_page_value = (
                    struct.unpack("<I", current_screen_page)[0]
                    if len(current_screen_page) == 4
                    else 0
                )
                manual_news_bridge = report.get("manual_news_ready_bridge")
                if (
                    isinstance(manual_news_bridge, dict)
                    and manual_news_bridge.get("status") == "installed"
                ):
                    current_handler_entry = read_process(
                        process.hProcess,
                        CLIENT_LITO_NEWS_HANDLER,
                        len(CLIENT_LITO_NEWS_HANDLER_ENTRY),
                    )
                    manual_news_bridge["handler_entry_current"] = (
                        current_handler_entry.hex(" ").upper()
                    )
                    manual_news_bridge["entry_hook_active"] = (
                        current_handler_entry
                        == bytes.fromhex(str(manual_news_bridge["entry_patch"]))
                    )
                    marker_address = int(
                        str(manual_news_bridge["pending_marker"]), 16
                    )
                    marker_raw = read_process(
                        process.hProcess, marker_address, 4
                    )
                    marker_value = (
                        struct.unpack("<I", marker_raw)[0]
                        if len(marker_raw) == 4
                        else 0
                    )
                    manual_news_bridge["pending_marker_value"] = marker_value
                    if marker_value == 1:
                        manual_news_bridge.setdefault(
                            "pending_observed_at_seconds", round(elapsed, 6)
                        )
                    if (
                        marker_value == 1
                        and screen_value != 0
                        and mode_value == 2
                    ):
                        invocation = invoke_manual_news_ready_bridge(
                            process.hProcess, manual_news_bridge
                        )
                        invocation["at_seconds"] = round(elapsed, 6)
                        invocation["ui_root"] = f"0x{screen_value:08X}"
                        manual_news_bridge["invocation"] = invocation
                        if invocation.get("status") == "native_handler_reinvoked":
                            manual_news_bridge["status"] = (
                                "native_handler_reinvoked"
                            )
                            manual_news_bridge["native_handler_reinvoked"] = True
                            manual_news_bridge["pending_marker_value"] = 2
                physical_page_bridge = report.get(
                    "physical_character_page_bridge"
                )
                if (
                    isinstance(physical_page_bridge, dict)
                    and physical_page_bridge.get("status")
                    in {"installed", "page_builder_returned"}
                    and physical_page_bridge.get("marker")
                ):
                    marker_address = int(
                        str(physical_page_bridge["marker"]), 16
                    )
                    marker_raw = read_process(
                        process.hProcess, marker_address, 4
                    )
                    marker_value = (
                        struct.unpack("<I", marker_raw)[0]
                        if len(marker_raw) == 4
                        else 0
                    )
                    hook_address = int(
                        str(
                            physical_page_bridge.get(
                                "hook_address",
                                f"0x{CLIENT_LITO_NEWS_HANDLER:08X}",
                            )
                        ),
                        16,
                    )
                    current_handler_entry = read_process(
                        process.hProcess,
                        hook_address,
                        len(CLIENT_LITO_NEWS_HANDLER_ENTRY),
                    )
                    physical_page_bridge["handler_entry_current"] = (
                        current_handler_entry.hex(" ").upper()
                    )
                    physical_page_bridge["entry_hook_active"] = (
                        current_handler_entry
                        == bytes.fromhex(
                            str(physical_page_bridge["entry_patch"])
                        )
                    )
                    physical_page_bridge["marker_value"] = marker_value
                    for address_key, value_key in (
                        ("invocation_count_address", "invocation_count"),
                        ("mode_snapshot_address", "mode_snapshot"),
                        ("page_snapshot_address", "page_snapshot"),
                        ("base_root_created_address", "base_root_created"),
                        (
                            "directory_root_created_address",
                            "directory_root_created",
                        ),
                    ):
                        value_raw = read_process(
                            process.hProcess,
                            int(str(physical_page_bridge[address_key]), 16),
                            4,
                        )
                        physical_page_bridge[value_key] = (
                            struct.unpack("<I", value_raw)[0]
                            if len(value_raw) == 4
                            else 0
                        )
                    expected_character_count_raw = read_process(
                        process.hProcess,
                        int(
                            str(
                                physical_page_bridge[
                                    "expected_character_count_address"
                                ]
                            ),
                            16,
                        ),
                        4,
                    )
                    physical_page_bridge["expected_character_count"] = (
                        struct.unpack("<I", expected_character_count_raw)[0]
                        if len(expected_character_count_raw) == 4
                        else 0
                    )
                    directory_slot_markers = b"".join(
                        read_process(
                            process.hProcess,
                            CLIENT_CHARACTER_DIRECTORY_RECORDS
                            + slot * CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE
                            + 0x2D,
                            2,
                        )
                        for slot in range(4)
                    )
                    directory_batch_complete = (
                        native_character_directory_batch_complete(
                            expected_character_count_raw,
                            character_directory_index,
                            character_directory_pending_count,
                            directory_slot_markers,
                        )
                    )
                    physical_page_bridge["character_directory_batch_complete"] = (
                        directory_batch_complete
                    )
                    physical_page_bridge["character_directory_slot_markers"] = (
                        directory_slot_markers.hex(" ").upper()
                    )
                    root_snapshot_raw = read_process(
                        process.hProcess,
                        int(
                            str(physical_page_bridge["root_snapshot_address"]),
                            16,
                        ),
                        4,
                    )
                    physical_page_bridge["root_snapshot"] = (
                        f"0x{struct.unpack('<I', root_snapshot_raw)[0]:08X}"
                        if len(root_snapshot_raw) == 4
                        else "0x00000000"
                    )
                    directory_root_snapshot_raw = read_process(
                        process.hProcess,
                        int(
                            str(
                                physical_page_bridge[
                                    "directory_root_snapshot_address"
                                ]
                            ),
                            16,
                        ),
                        4,
                    )
                    physical_page_bridge["directory_root_snapshot"] = (
                        f"0x{struct.unpack('<I', directory_root_snapshot_raw)[0]:08X}"
                        if len(directory_root_snapshot_raw) == 4
                        else "0x00000000"
                    )
                    if marker_value == 2:
                        physical_page_bridge.setdefault(
                            "page_builder_returned_at_seconds",
                            round(elapsed, 6),
                        )
                        physical_page_bridge["status"] = "page_builder_returned"
                    initial_activation = physical_page_bridge.get(
                        "initial_character_row_activation"
                    )
                    if (
                        isinstance(initial_activation, dict)
                        and marker_value == 2
                        and current_page_value == 7
                        and bool(directory_name)
                        and directory_batch_complete
                    ):
                        page_builder_invocation = int(
                            physical_page_bridge.get("invocation_count", 0)
                        )
                        last_activated_invocation = int(
                            initial_activation.get(
                                "last_activated_page_builder_invocation", 0
                            )
                        )
                        observed_invocation = int(
                            initial_activation.get(
                                "directory_complete_observed_invocation", 0
                            )
                        )
                        if observed_invocation != page_builder_invocation:
                            initial_activation[
                                "directory_complete_observed_invocation"
                            ] = page_builder_invocation
                            initial_activation[
                                "directory_complete_observed_at_seconds"
                            ] = round(elapsed, 6)
                            initial_activation[
                                "attempts_for_current_invocation"
                            ] = 0
                        complete_at = float(
                            initial_activation.get(
                                "directory_complete_observed_at_seconds",
                                elapsed,
                            )
                        )
                        stable_delay = float(
                            initial_activation.get(
                                "delay_after_complete_seconds", 0.5
                            )
                        )
                        attempts_for_invocation = int(
                            initial_activation.get(
                                "attempts_for_current_invocation", 0
                            )
                            if observed_invocation == page_builder_invocation
                            else 0
                        )
                        attempt_interval = float(
                            initial_activation.get(
                                "attempt_interval_seconds", 1.0
                            )
                        )
                        attempts_per_directory = int(
                            initial_activation.get("attempts_per_directory", 2)
                        )
                        if (
                            page_builder_invocation > last_activated_invocation
                            and attempts_for_invocation < attempts_per_directory
                            and elapsed - complete_at
                            >= stable_delay
                            + attempts_for_invocation * attempt_interval
                        ):
                            selected_index_raw = read_process(
                                process.hProcess,
                                CLIENT_CHARACTER_SELECTED_INDEX,
                                4,
                            )
                            selected_index = (
                                struct.unpack("<I", selected_index_raw)[0]
                                if len(selected_index_raw) == 4
                                else 0
                            )
                            activation_result = (
                                post_native_character_row_selection(
                                    root_pid, selected_index
                                )
                            )
                            activation_result["at_seconds"] = round(elapsed, 6)
                            activation_result[
                                "page_builder_invocation"
                            ] = page_builder_invocation
                            activation_result["attempt_count"] = int(
                                initial_activation.get("attempt_count", 0)
                            ) + 1
                            initial_activation.update(activation_result)
                            if bool(activation_result.get("posted")):
                                attempts_for_invocation += 1
                                initial_activation[
                                    "attempts_for_current_invocation"
                                ] = attempts_for_invocation
                            if attempts_for_invocation >= attempts_per_directory:
                                initial_activation[
                                    "last_activated_page_builder_invocation"
                                ] = page_builder_invocation
                if (
                    args.manual_native_selection
                    and manual_native_screen_baseline is None
                    and mode_value == 2
                    and screen_value != 0
                ):
                    manual_native_screen_baseline = screen_value
                physical_page_builder_returned = (
                    isinstance(physical_page_bridge, dict)
                    and physical_page_bridge.get("status")
                    == "page_builder_returned"
                )
                ui_rearm_trigger = native_channel_ui_rearm_trigger(
                    manual_native_selection=args.manual_native_selection,
                    physical_native_login_session=physical_native_login_session,
                    manual_native_screen_baseline=manual_native_screen_baseline,
                    screen_value=screen_value,
                    server_list_value=server_list_value,
                    phase_value=phase_value,
                    mode_value=mode_value,
                    current_page_value=current_page_value,
                    directory_present=bool(directory_name),
                    physical_page_builder_returned=physical_page_builder_returned,
                )
                if (
                    server_profile is not None
                    and args.manual_native_selection
                    and ui_rearm_trigger is not None
                    and len(game_ui_active) == 1
                    and game_ui_active[0] == 0
                ):
                    written = write_process(
                        process.hProcess, CLIENT_GAME_UI_ACTIVE_FLAG, b"\x01"
                    )
                    if written != 1:
                        raise RuntimeError("could not arm native channel UI transition")
                    game_ui_active = b"\x01"
                    vs_lobby_ui_rearmed = True
                    activation = {
                        "status": "armed_before_native_channel_transition",
                        "at_seconds": round(elapsed, 6),
                        "flag_address": f"0x{CLIENT_GAME_UI_ACTIVE_FLAG:08X}",
                        "client_mode": mode_value,
                        "channel_screen": f"0x{screen_value:08X}",
                        "current_screen_page": current_page_value,
                        "trigger": ui_rearm_trigger,
                        "executable_modified": False,
                    }
                    report["vs_lobby_ui_activation"] = activation
                    report.setdefault("vs_lobby_ui_activation_history", []).append(
                        activation.copy()
                    )
                if (
                    server_profile is not None
                    and not args.manual_native_selection
                    and not vs_lobby_ui_rearmed
                    and len(character_auto_select_sent) == 1
                    and character_auto_select_sent[0] == 1
                    and len(client_phase) == 4
                    and struct.unpack("<I", client_phase)[0] == 0
                    and len(game_ui_active) == 1
                    and game_ui_active[0] == 0
                ):
                    written = write_process(
                        process.hProcess, CLIENT_GAME_UI_ACTIVE_FLAG, b"\x01"
                    )
                    if written != 1:
                        raise RuntimeError("could not re-arm VS lobby UI transition")
                    game_ui_active = b"\x01"
                    vs_lobby_ui_rearmed = True
                    report["vs_lobby_ui_activation"] = {
                        "status": "rearmed_before_enter_channel_response",
                        "at_seconds": round(elapsed, 6),
                        "flag_address": f"0x{CLIENT_GAME_UI_ACTIVE_FLAG:08X}",
                        "trigger": "native character auto-select sent while client phase is zero",
                        "executable_modified": False,
                    }
                telemetry: dict[str, Any] = {}
                if len(fullscreen_flag) == 1:
                    flag_value = fullscreen_flag[0]
                    telemetry["fullscreen_flag"] = flag_value
                    report["display_mode"]["last_observed_fullscreen_flag"] = flag_value
                if len(product_version) == 4:
                    telemetry["client_product_version"] = (
                        f"0x{struct.unpack('<I', product_version)[0]:08X}"
                    )
                if len(build_version) == 4:
                    telemetry["client_build_version"] = (
                        f"0x{struct.unpack('<I', build_version)[0]:08X}"
                    )
                for name, raw in (
                    ("client_mode", client_mode),
                    ("client_phase", client_phase),
                    ("server_protocol_version", server_protocol_version),
                    ("current_screen", current_screen),
                    ("server_list", server_list),
                    ("world_scene", world_scene),
                    ("static_scene", static_scene),
                ):
                    if len(raw) == 4:
                        telemetry[name] = f"0x{struct.unpack('<I', raw)[0]:08X}"
                telemetry["arena_start_globals"] = {
                    name: f"0x{struct.unpack('<I', raw)[0]:08X}"
                    for name, raw in arena_start_globals.items()
                    if len(raw) == 4
                }
                telemetry["native_source_modal"] = {
                    "handlers": {
                        name: f"0x{struct.unpack('<I', raw)[0]:08X}"
                        for name, raw in native_source_handlers.items()
                        if len(raw) == 4
                    },
                    "expected_handlers": {
                        "key": f"0x{CLIENT_NATIVE_SOURCE_MODAL_KEY_HANDLER:08X}",
                        "mouse": f"0x{CLIENT_NATIVE_SOURCE_MODAL_MOUSE_HANDLER:08X}",
                        "render": f"0x{CLIENT_NATIVE_SOURCE_MODAL_RENDER_HANDLER:08X}",
                    },
                    "root": (
                        f"0x{struct.unpack('<I', native_source_modal_root)[0]:08X}"
                        if len(native_source_modal_root) == 4
                        else None
                    ),
                    "selected_index": (
                        struct.unpack('<I', native_source_selected_index)[0]
                        if len(native_source_selected_index) == 4
                        else None
                    ),
                    "source_count": (
                        native_source_count[0]
                        if len(native_source_count) == 1
                        else None
                    ),
                }
                if ready_world_transition:
                    telemetry["ready_world_transition"] = ready_world_transition
                for name, raw in (
                    ("login_server_flag", login_server_flag),
                    ("character_server_flag", character_server_flag),
                    ("character_auto_select_sent", character_auto_select_sent),
                    ("character_server_mode", character_server_mode),
                    ("special_channel_mode_a", special_channel_mode_a),
                    ("special_channel_mode_b", special_channel_mode_b),
                    ("auto_enter_channel", auto_enter_channel),
                    ("game_ui_active", game_ui_active),
                    ("lito_news_defer", lito_news_defer),
                    ("native_source_cached_select", native_source_cached_select),
                    ("native_source_force_selector", native_source_force_selector),
                ):
                    if len(raw) == 1:
                        telemetry[name] = raw[0]
                if len(current_screen_page) == 4:
                    telemetry["current_screen_page"] = struct.unpack(
                        "<I", current_screen_page
                    )[0]
                if len(character_directory_record) == CLIENT_CHARACTER_DIRECTORY_RECORD_STRIDE:
                    telemetry["character_directory"] = {
                        "first_record_present": bool(directory_name),
                        "first_record_name_length": len(directory_name),
                        "first_record_name_sha256": (
                            hashlib.sha256(directory_name).hexdigest().upper()
                            if directory_name
                            else None
                        ),
                        "first_record_nonzero_bytes": sum(
                            byte != 0 for byte in character_directory_record
                        ),
                        "selection_state_2d": character_directory_record[0x2D],
                        "selection_state_2e": character_directory_record[0x2E],
                        "selection_state_37": character_directory_record[0x37],
                        "parse_index": (
                            struct.unpack("<I", character_directory_index)[0]
                            if len(character_directory_index) == 4
                            else None
                        ),
                        "pending_count": (
                            struct.unpack(
                                "<I", character_directory_pending_count
                            )[0]
                            if len(character_directory_pending_count) == 4
                            else None
                        ),
                        "character_name_logged": False,
                    }
                if telemetry:
                    sample["client_telemetry"] = telemetry
                if args.capture_contexts and elapsed >= next_context_sample:
                    sample["thread_contexts"] = sample_thread_contexts(
                        root_pid, process.hProcess
                    )
                    next_context_sample = elapsed + args.context_interval
                report["samples"].append(sample)
                next_sample = elapsed + args.interval

            if transfer_probe_server_click_at is not None:
                transfer_probe = report.get("transfer_stone_native_producer")
                physical_page_bridge = report.get(
                    "physical_character_page_bridge"
                )
                if (
                    transfer_probe_battle_character_page_at is None
                    and isinstance(transfer_probe, dict)
                    and isinstance(physical_page_bridge, dict)
                    and transfer_probe_character_page_baseline is not None
                    and int(physical_page_bridge.get("invocation_count", 0))
                    > transfer_probe_character_page_baseline
                ):
                    transfer_probe_battle_character_page_at = elapsed
                    transfer_probe["battle_endpoint_character_page"] = {
                        "status": "native_page_7_character_row_ready",
                        "page_builder_invocation_before": (
                            transfer_probe_character_page_baseline
                        ),
                        "page_builder_invocation_after": int(
                            physical_page_bridge.get("invocation_count", 0)
                        ),
                        "at_seconds": round(elapsed, 6),
                        "physical_selection_required": False,
                        "continuation": (
                            "destination server consumes the one-shot handoff and "
                            "reuses the native S_C8 -> C_CA(0) continuation"
                        ),
                    }
                if (
                    elapsed - transfer_probe_server_click_at
                    >= args.transfer_stone_settle_seconds
                ):
                    if transfer_probe_battle_world_at is not None:
                        report["status"] = (
                            "transfer_stone_battle_world_evidence_captured"
                        )
                    elif transfer_probe_battle_character_page_at is not None:
                        report["status"] = (
                            "transfer_stone_battle_character_page_evidence_captured"
                        )
                    else:
                        report["status"] = (
                            "transfer_stone_directory_selection_evidence_captured"
                        )
                    if isinstance(transfer_probe, dict):
                        transfer_probe["settle_window"] = {
                            "configured_seconds": args.transfer_stone_settle_seconds,
                            "elapsed_seconds": round(
                                elapsed - transfer_probe_server_click_at, 6
                            ),
                            "battle_character_page_observed": (
                                transfer_probe_battle_character_page_at is not None
                            ),
                            "battle_world_observed": (
                                transfer_probe_battle_world_at is not None
                            ),
                        }
                    kernel32.TerminateJobObject(job, 0)
                    report["exit_code"] = 0
                    report["exit_code_hex"] = "0x00000000"
                    break

            if not kernel32.GetExitCodeProcess(process.hProcess, ctypes.byref(exit_code)):
                raise win_error("GetExitCodeProcess")
            if exit_code.value != STILL_ACTIVE:
                if report.get("connect_redirect", {}).get("status") == "installed":
                    update_connect_redirect_telemetry(
                        process.hProcess, report["connect_redirect"]
                    )
                if report.get("window_close_trace", {}).get("status") == "installed":
                    update_window_close_trace(
                        process.hProcess, report["window_close_trace"]
                    )
                report["status"] = "exited"
                report["exit_code"] = int(exit_code.value)
                report["exit_code_hex"] = f"0x{int(exit_code.value):08X}"
                if args.post_root_exit_wait > 0:
                    observation_started = time.monotonic()
                    observations: list[dict[str, Any]] = []
                    while time.monotonic() - observation_started < args.post_root_exit_wait:
                        table = process_snapshot()
                        child_pids = descendants(root_pid, table) - {root_pid}
                        child_rows: list[dict[str, Any]] = []
                        for pid in sorted(child_pids):
                            row = dict(table.get(pid, {"pid": pid}))
                            row["path"] = process_path(pid)
                            modules = module_snapshot(pid)
                            row["module_count"] = len(modules)
                            child_rows.append(row)
                            if (
                                args.launcher_child_mode
                                and Path(row["path"]).name.casefold() == "lito.bin"
                            ):
                                if not launcher_child_handle:
                                    child_sha256 = hashlib.sha256(
                                        Path(row["path"]).read_bytes()
                                    ).hexdigest().upper()
                                    if child_sha256 != KNOWN_PROTECTED_SHA256:
                                        raise ValueError(
                                            "launcher child is not the verified original LiTo.bin"
                                        )
                                    launcher_child_handle = kernel32.OpenProcess(
                                        PROCESS_ALL_ACCESS, False, pid
                                    )
                                    if not launcher_child_handle:
                                        raise win_error("OpenProcess(launcher child)")
                                    report["launcher_child"] = {
                                        "pid": pid,
                                        "path": row["path"],
                                        "sha256": child_sha256,
                                        "command_line_logged": False,
                                    }
                                    report["launcher_child_connect_redirect"] = {
                                        "status": "waiting_for_decrypted_connect_iat",
                                        "profile": {
                                            key: value
                                            for key, value in server_profile.items()
                                            if key != "packed_ip"
                                        },
                                    }
                                child_redirect = report[
                                    "launcher_child_connect_redirect"
                                ]
                                if child_redirect.get("status") == "installed":
                                    update_connect_redirect_telemetry(
                                        launcher_child_handle, child_redirect
                                    )
                                else:
                                    installed_child_redirect = install_connect_redirect(
                                        launcher_child_handle,
                                        pid,
                                        server_profile,
                                        args.manual_native_selection,
                                        None,
                                        child_redirect,
                                    )
                                    if installed_child_redirect is not None:
                                        installed_child_redirect[
                                            "installed_seconds_after_root_exit"
                                        ] = round(
                                            time.monotonic() - observation_started, 3
                                        )
                                        report[
                                            "launcher_child_connect_redirect"
                                        ] = installed_child_redirect
                            if pid not in seen_pids:
                                seen_pids.add(pid)
                                report["processes"].append(
                                    {
                                        "first_seen_seconds": round(
                                            time.monotonic() - started, 3
                                        ),
                                        **row,
                                    }
                                )
                            for module in modules:
                                key = (pid, module["path"].casefold(), module["base"])
                                if key not in seen_modules:
                                    seen_modules.add(key)
                                    report["modules"].append(
                                        {
                                            "first_seen_seconds": round(
                                                time.monotonic() - started, 3
                                            ),
                                            "pid": pid,
                                            **module,
                                        }
                                    )
                        observations.append(
                            {
                                "seconds_after_root_exit": round(
                                    time.monotonic() - observation_started, 3
                                ),
                                "children": child_rows,
                            }
                        )
                        time.sleep(0.1)
                    report["post_root_exit_observations"] = observations
                    if any(row["children"] for row in observations):
                        report["status"] = "root_exited_child_observed"
                if args.allow_wer:
                    wer_started = time.monotonic()
                    while time.monotonic() - wer_started < 20.0:
                        rows = process_snapshot()
                        remaining = descendants(root_pid, rows) - {root_pid}
                        if not remaining:
                            break
                        time.sleep(0.1)
                    report["wer_completion_wait_seconds"] = round(
                        time.monotonic() - wer_started, 3
                    )
                break
            shared = report.get("shared_resources")
            if shared and shared.get("gate_released"):
                for name, address in shared.get("tbt_statistics",{}).items():
                    counter = read_process(process.hProcess,int(address,16),4)
                    if len(counter)==4: shared[name] = struct.unpack("<I",counter)[0]
                if shared.get("resource_trace_address"):
                    import native_resource_trace
                    raw_trace = read_process(process.hProcess, int(shared["resource_trace_address"],16),
                                             16 + native_resource_trace.SLOTS * native_resource_trace.STRIDE)
                    if len(raw_trace) == 16 + native_resource_trace.SLOTS * native_resource_trace.STRIDE:
                        shared["resource_trace"] = native_resource_trace.decode(raw_trace)
                raw_state = read_process(process.hProcess, int(shared["state_address"],16), 4)
                if len(raw_state) == 4:
                    shared["native_state"] = struct.unpack("<I", raw_state)[0]
                    shared["native_base_count"] = struct.unpack("<I", read_process(process.hProcess,0x647E90,4))[0]
                    shared["native_total_count"] = struct.unpack("<I", read_process(process.hProcess,0x647E7C,4))[0]
                    raw_bounds = read_process(process.hProcess,int(shared["bounds_diagnostics"],16),12)
                    shared["rejected_effects"], shared["last_rejected_effect"], shared["discarded_effect_displays"] = struct.unpack("<III",raw_bounds)
                    shared["native_cleanup_complete"] = struct.unpack("<I", read_process(
                        process.hProcess,int(shared["parser_completion_address"],16),4))[0] == 1
                    shared["live_parser_verified"] = (shared["native_cleanup_complete"] and shared["native_state"] == 3 and
                        shared["native_base_count"] == 30000 and shared["native_total_count"] == 30285)
                    if shared["live_parser_verified"] and not shared.get("ready_receipt_written"):
                        receipt = args.output.with_suffix(".shared-ready.json")
                        with receipt.open("x", encoding="utf-8") as stream:
                            json.dump({"pid": root_pid, "profile": "tw13081901-v1",
                                       "base_count": 30000, "total_count": 30285,
                                       "native_cleanup_complete": True}, stream)
                        shared["ready_receipt_written"] = True
                    if shared["native_state"] >= 0xE0:
                        raise RuntimeError("common catalog native loader rejected state: " + hex(shared["native_state"]))
            if args.timeout > 0 and elapsed >= args.timeout:
                report["status"] = (
                    "exit_intercepted_terminated"
                    if report.get("process_exit_trace", {}).get("status")
                    == "exit_intercepted"
                    else "timeout_terminated"
                )
                kernel32.TerminateJobObject(job, args.timeout_exit_code)
                report["exit_code"] = args.timeout_exit_code
                report["exit_code_hex"] = f"0x{args.timeout_exit_code:08X}"
                break
            time.sleep(min(args.poll_interval, args.interval))
    except Exception as exc:
        report["status"] = "probe_error"
        report["error"] = f"{type(exc).__name__}: {exc}"
        raise
    finally:
        credential_bootstrap = report.get("credential_bootstrap")
        if isinstance(credential_bootstrap, dict):
            credential_bootstrap.pop("_restore_account", None)
            credential_bootstrap.pop("_restore_password", None)
        report["finished_at"] = datetime.now().astimezone().isoformat()
        if launcher_credentials is not None:
            for secret in launcher_credentials:
                secret[:] = b"\x00" * len(secret)
        if game_handoff_credentials is not None:
            for secret in game_handoff_credentials:
                secret[:] = b"\x00" * len(secret)
        if old_shared_gate is None:
            os.environ.pop("LITO_SHARED_RESOURCE_GATE", None)
        else:
            os.environ["LITO_SHARED_RESOURCE_GATE"] = old_shared_gate
        if old_compat_layer is None:
            os.environ.pop("__COMPAT_LAYER", None)
        else:
            os.environ["__COMPAT_LAYER"] = old_compat_layer
        kernel32.SetErrorMode(previous_error_mode)
        if process.hThread:
            kernel32.CloseHandle(process.hThread)
        if process.hProcess:
            kernel32.CloseHandle(process.hProcess)
        if launcher_child_handle:
            kernel32.CloseHandle(launcher_child_handle)
        if job:
            kernel32.CloseHandle(job)
        report.pop("_debug_initial_breakpoint_seen", None)
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(
            json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8"
        )

    summary = {
        "status": report["status"],
        "root_pid": report.get("root_pid"),
        "exit_code_hex": report.get("exit_code_hex"),
        "process_count": len(report["processes"]),
        "module_count": len(report["modules"]),
        "report": str(args.output.resolve()),
    }
    print(json.dumps(summary, ensure_ascii=False, indent=2))
    return 0


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--executable", type=Path, required=True)
    parser.add_argument("--cwd", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument(
        "--timeout",
        type=float,
        default=20.0,
        help="seconds before terminating the job; use 0 to wait until the client exits",
    )
    parser.add_argument("--interval", type=float, default=0.25)
    parser.add_argument(
        "--poll-interval",
        type=float,
        default=0.05,
        help="seconds between fast patch/marker polls",
    )
    parser.add_argument("--capture-contexts", action="store_true")
    parser.add_argument(
        "--allow-wer",
        action="store_true",
        help="allow Windows Error Reporting to collect a configured local crash dump",
    )
    parser.add_argument(
        "--debug-exceptions",
        action="store_true",
        help="capture the exact second-chance exception through Win32 debug events",
    )
    parser.add_argument(
        "--trace-window-close",
        action="store_true",
        help="diagnostically record DestroyWindow/quit callers during UI bring-up",
    )
    parser.add_argument(
        "--trace-process-exit",
        action="store_true",
        help=(
            "diagnostically pause at WOW64 RtlExitUserProcess and capture the "
            "live caller stack before timeout termination"
        ),
    )
    parser.add_argument("--multi-instance-startup-gate", action="store_true")
    parser.add_argument("--context-interval", type=float, default=1.0)
    parser.add_argument("--dump-image", type=Path)
    parser.add_argument("--dump-at", type=float, default=2.0)
    parser.add_argument("--dump-base", type=lambda v: int(v, 0), default=0x00400000)
    parser.add_argument("--dump-size", type=lambda v: int(v, 0), default=0x006C4000)
    parser.add_argument("--patch", type=parse_patch, action="append")
    parser.add_argument("--conditional-patch", type=parse_conditional_patch, action="append")
    parser.add_argument(
        "--display-mode",
        choices=("auto", "windowed", "fullscreen"),
        default="auto",
        help=(
            "override the verified client's DirectDraw mode after runtime decryption; "
            "auto preserves its desktop-color-depth decision"
        ),
    )
    parser.add_argument("--patch-at", type=float, default=1.0)
    parser.add_argument("--install-gap-veh", action="store_true")
    parser.add_argument(
        "--veh-scan-instruction", type=lambda v: int(v, 0), default=0x00A48614
    )
    parser.add_argument(
        "--veh-entry-point", type=lambda v: int(v, 0), default=0x0083B014
    )
    parser.add_argument("--trace-oep", action="store_true")
    parser.add_argument("--shared-resource-candidate", type=Path,
                        help="staged common catalog using the graphics-init gate; no publication implied")
    parser.add_argument(
        "--hardware-breakpoint",
        type=lambda v: int(v, 0),
        help="set one x86 execute breakpoint when the conditional patch is applied",
    )
    parser.add_argument(
        "--capture-breakpoint",
        type=parse_capture_breakpoint,
        help="after decryption, replace one expected byte with INT3 and capture its context",
    )
    parser.add_argument(
        "--capture-packet-opcode",
        type=lambda v: int(v, 0),
        help=(
            "after decryption, capture and park the first original packet-writer "
            "call whose opcode exactly matches this byte"
        ),
    )
    parser.add_argument("--timeout-exit-code", type=lambda v: int(v, 0), default=0xC0DE0001)
    parser.add_argument(
        "--post-root-exit-wait",
        type=float,
        default=0.0,
        help="keep the kill-on-close job alive briefly to observe launcher children",
    )
    parser.add_argument(
        "--launcher-child-mode",
        action="store_true",
        help=(
            "launch the verified original LiTo.exe and install the loopback "
            "connect redirect in its verified LiTo.bin child"
        ),
    )
    parser.add_argument("--affinity-mask", type=lambda v: int(v, 0))
    parser.add_argument("--compat-layer")
    parser.add_argument(
        "--server-profile",
        type=Path,
        help="JSON server profile used to redirect IPv4 connect() calls after decryption",
    )
    parser.add_argument(
        "--allow-non-loopback-redirect",
        action="store_true",
        help="permit a server profile outside 127.0.0.0/8; use only with an authorized server",
    )
    parser.add_argument(
        "--manual-native-selection",
        action="store_true",
        help=(
            "preserve LiTo's native server and character selection instead of "
            "re-arming the compatibility auto-enter flags"
        ),
    )
    parser.add_argument(
        "--physical-native-login",
        action="store_true",
        help=(
            "reserve LITO381_ACCOUNT/PASSWORD only for each connect-time "
            "official-launcher handoff; do not auto-submit the first login, "
            "so the original login button can construct the native UI flow"
        ),
    )
    parser.add_argument(
        "--invoke-transfer-stone-producer",
        action="store_true",
        help=(
            "isolated loopback-only diagnostic: after native phase 4, locate "
            "session object 0x6C000001 and invoke the original sub_445BD0 "
            "C_USE_ITEM producer once"
        ),
    )
    parser.add_argument(
        "--transfer-stone-settle-seconds",
        type=float,
        default=2.0,
        help=(
            "seconds to keep the native client alive after the transfer-stone "
            "server row is selected, allowing physical battle-endpoint "
            "character selection during end-to-end validation"
        ),
    )
    parser.add_argument(
        "--invoke-rental-town-room-selector",
        action="store_true",
        help=(
            "loopback-only diagnostic: wait for one S_FB group-1 room received "
            "through the normal socket path, then invoke the original native "
            "sub_475DA0 down/up selector without claiming human input"
        ),
    )
    parser.add_argument(
        "--skip-surface-precedence",
        action="store_true",
        help=(
            "do not install the SpriteXX/LTsurf collision hook when the working "
            "directory already contains the verified legacy-compatible archives"
        ),
    )
    parser.add_argument(
        "--native-arena-transition",
        dest="native_arena_transition",
        action="store_true",
        help=(
            "preserve the verified client's native READY and START handlers "
            "(the default)"
        ),
    )
    parser.add_argument(
        "--compat-arena-phase-only",
        dest="native_arena_transition",
        action="store_false",
        help=(
            "diagnostic-only legacy fallback that changes phase without "
            "constructing or entering the native arena"
        ),
    )
    parser.set_defaults(native_arena_transition=True)
    parser.add_argument("arguments", nargs="*")
    return parser.parse_args()


if __name__ == "__main__":
    raise SystemExit(run_probe(parse_args()))
