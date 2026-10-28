# PlayerID iOS Host

This directory is the thin native host for the shared `PlayerIDShared` framework. Product rules and portable Compose UI belong in `shared`; Swift should only own Apple lifecycle and native service adapters.

## Ask Playloom: audit and Phase 1 metadata-search MVP

### Playloom Go entry

Non-memory Go answers (including roster and schedule questions) hide the ordinary
library filters and collection tabs. Schedule answers show past saved memories
against the returned opponent(s), matched within each answered team, instead of
leaving the previous memory search/grid underneath. Related results ignore
previous player/season/opponent filters, exclude reels and future-dated memories,
and use only saved opponent/team assignments. No match shows a concise notice;
no scheduled game means no guessed opponent or unrelated grid. Roster and other
non-memory answers show the answer without a generic library underneath.

Memories uses a two-row filter layout: Player and Season first, Team and
Opponent second. Each control takes half the available row and shows its category
above the selected value. Long values stay on one line and truncate with ellipsis; active
filters use pale blue while "All" controls use light gray. The minimum control
height is fixed at 41.6dp (20% below 52dp), with 4.8dp between rows; text cannot
increase pill height. Dropdowns retain full names. No horizontal scrolling is required. Filters collapse
when scrolling down through memories and reappear after a deliberate 140dp pull
toward the top in one gesture. Small pulls do not accumulate across gestures;
momentum alone cannot reveal filters. Direction changes reset the distance.
Returning to the very top automatically reveals filters, including after a fling,
without requiring the full pull distance.
The collapse threshold is 8dp; both thresholds scale with display density.
They stay available while selecting memories.

The bottom menu has Camera on the left, raised **Go** at the exact screen
center, and Memories on the right. A single three-dot button at the right of
the Memories/Reels header replaces +, Select and the gear. Its dropdown has
Upload memory, the existing remove/create-reel/share selection actions, and
Teams (opening team management directly), and Settings & more. Selection mode retains its Cancel control. Settings & more
opens the Settings and Account menu without a separate "More" page heading.
Teams is available directly in the three-dot menu, not repeated inside Settings & more.
iPhone menu pages have a back control;
the menu returns to Memories and Teams/Settings return to the menu.
Go is a 68dp theme-colored circle with a single sparkle/stars icon (no visible
"Go" label) and the accessibility name "Playloom Go", above the ordinary 60dp menu row, and
stays prominent whether selected or not. It opens a dedicated **Playloom Go**
page with "Ask, find, create...", microphone, inline answers and
matching memory/reel results. The search entry is removed from the ordinary
Memories/Reels header. Both destinations reuse the existing shared library
workflows and native adapters; no second search engine or media store is added.
Go and Memories maintain separate transient search/view state. Opening Go does
not start recording audio; the user taps the mic. Navigation shortcuts, roster
and schedule answers, reel review and recipient confirmation remain unchanged.
The existing landscape-camera bottom-menu hiding behavior is retained.

The Go landing page follows the dedicated assistant layout: sparkle mark,
"Your sports memory assistant", the input,
eight full-width "Try these" rows (one suggestion per line), and real recent requests.
The heading sparkles and favorite-example star are bright blue. No library filters,
collection tabs or media grid appear on the landing page. Typing stays there
until Search is submitted; requests then open the existing answers/results
presentation. Its back arrow returns to the landing page. Recent contains the
last four distinct submitted requests for this saved screen session, newest
first; tapping one reruns it, and Clear removes the list. No sample history or
fictional player/recipient records are inserted. Sharing examples prepare a reel
for review and still require explicit sharing confirmation.

Suggestion rows use a compact 40dp minimum visible height, 4dp vertical content
padding and 2dp gaps. The extra "Find, create, share..." description is removed.

Every tap of the bottom Go button opens a fresh landing page, even when Go is
already selected or a previous result/viewer/editor is open. Recent requests
are kept separately from that reset. Unsaved Go drafts are dismissed; saved
memories and reels are not changed.

On a physical phone, check the raised Go button from Memories, Camera and More;
open Go, type and dictate questions, view a matching memory, create/cancel a reel
draft, and switch back to Memories. Confirm no search bar remains at the top
of Memories and Go's query does not filter that ordinary library.

Navigation shortcuts work by typing or voice: "Open our roster", "Show our
schedule", "Open Tigers' roster", "Open the schedule for Tigers", "Open teams",
"Open camera", "Open reels", and "Open memories". Roster/schedule requests use
the selected library team, or the only available team; with multiple teams and
no selection, the inline answer asks for a full team name. Named teams must
match an available team exactly (case-insensitive). Navigation opens existing
screens without modifying game setup or roster records. Reels/Memories switch
the current library tab and clear the query. Memory-result follow-ups and
missing-detail searches are not added.

The shared entry is a single 48dp bar with a microphone and query-clear control;
the question-mark help button and its dialog are removed. The landing page's
suggestions submit through the existing typed/voice request route; reel requests
still require review. Examples use a saved player hint when available and generic
requests otherwise, never placeholder names. Opening Go does not automatically
start microphone recording.

Playloom Go is the dedicated entry page for Ask Playloom, reusing the existing
Memories/Reels engine rather than a chatbot or separate data dashboard.
The following search behavior is implemented on Android and iPhone:

- The compact shared bar reads **Ask, find, create...**, with a prominent filled
  microphone button. Tap the text area to type; the full-height text area remains
  editable without opening a separate screen. Example suggestions appear in
  the landing page's "Try these" list. Voice entry does not open the keyboard.
- Submitting from the Go landing page opens the existing grid/answers; typing
  stays on the landing page until submission. Once results are open, edits
  update that grid. Speech-to-text submits through the same route. Existing
  voice permissions and one-shot recognition remain.
- The results header shows the number of matching memories/reels and removable
  interpretation chips on Reels results inside Go. Memories results hide the
  chips; edit the request or use the field's X to clear it. On Reels, tap a
  chip's close control to remove that constraint.
- Memories search results omit the Play all / Create reel / Share shortcut row.
  Existing selection menus and typed/spoken reel requests remain available.
- Memories also omit the result-count label and separate Clear search button;
  use the X in the Ask Playloom field to clear the request. Question answers,
  clarification/errors and no-match notices remain visible.
- Zero-match memory count questions show only the total (for example
  "0 recorded faceoff wins") in the answer card, without explanatory or
  duplicate empty-library text below it.
- Exact/full names, unique partial names and dictated possessives resolve
  against roster records. Ambiguous names show clarification, not mixed results.
  Include a team name when the same name has multiple roster entries. Two
  children with different names remain independently searchable.
- Recognized player/opponent/date/season requests clear conflicting manual
  player/opponent/season filters. Other visible library filters remain active.
  Android stays within the loaded team or All Teams; choose All Teams to search
  across teams. Neither platform automatically switches Memories/Reels tabs.
- Player search uses **saved assignments**, not background detections or a name
  found in a note. An assignment is not a newly verified participation claim.
  Reels match their source assignments/tags, keeping the requested player and
  moment on the same source.
- Moment types, stars, media types, opponents, calendar dates, seasons, first/
  latest and residual text constraints combine. Residual text includes notes.
  Unknown words remain visible text constraints rather than being discarded.
- Bare years/"from 2025" mean the capture's calendar year, not any mention of
  2025 in metadata. "2025 season" means the saved season year. "This season"
  uses the selected library season, or the current calendar-year season if none
  is selected. "Last season" means the previous   calendar-year season. When no season is assigned, the known recording year
  supplies the default; an explicitly assigned season always takes precedence.
- "Find Tyson's first goal of 2026" selects the earliest matching saved goal
  within January 1-December 31, 2026. Current-tab, reel-year and video-picker
  constraints apply before first/latest ranking, so a hidden reel/photo cannot
  consume the single result and leave the visible collection empty.
- "Last weekend" means Saturday/Sunday of the preceding calendar week.
  "This summer" means June 1-August 31 of this year; "last summer" means the
  most recent completed such summer. Explicit "summer 2025" is supported.
  Dates use the device's local calendar and the chips show actual boundaries.
- Shared-with-me/ownership and detection-specific requests explicitly say that
  their metadata is not connected yet and return no inferred matches.

### Reused components and new boundaries

| Concern | Implementation |
| --- | --- |
| Interpretation | New shared `AskPlayloomQuery`, wrapping the existing `MemorySearchQuery`, with resolved roster/team/opponent constraints and a clarification state |
| Editable interpretation | New shared `AskPlayloomResults`; removable chips only change transient query state |
| Query execution | Existing `searchCaptureLibrary` against the local repository snapshot on iPhone; existing Android restored-clip matcher against Room/local metadata, without detection bubbles for Ask searches |
| Existing UI | Shared `CaptureLibrarySearchBar`, iPhone `CameraCaptureLibraryPage`, Android `ClipsScreenRefactored`; Go presentation reuses cards, playback and actions behind a dedicated bottom destination |
| Later actions | Reuse `LibraryActionRequest`, existing reel editor, `VideoPlaybackScreen`, iPhone native playback, and existing multi-memory/guardian sharing |
| Data/account boundary | Existing `LocalTeamRepository`, Android Room repositories, and Supabase email authentication |

No second player/memory persistence model, new database, video analysis,
embedding pipeline, hosted AI key or network dependency was added. The query is
a transient typed contract. A later backend executor must use authorized
repository records; interpretation must never grant access or generate SQL.
Supabase currently implements authentication only. Shared-memory records,
ownership, confirmed-vs-detected associations, family authorization and RLS
integration must be implemented before cloud/private-source search is enabled.
Original-media storage is unchanged. Missing files retain the existing native
loading/playback error behavior; Phase 1 does not download or reconstruct media.

### Milestones

1. **Phase 1 (this change):** structured metadata retrieval, existing grids,
   visible/removable interpretation, local date handling and accuracy guards.
2. **Phase 2 (implemented):** grounded count/opponent/first-recorded answer cards
   with supporting clips and recorded-event wording, using the same metadata
   interpretation and visible-result execution as retrieval.
3. **Phase 3 (implemented):** temporary Play All through existing players,
   without exporting or saving a reel, on both Android and iPhone.
4. **Phase 4 (implemented):** result actions for reviewed reel drafts and
   multi-select sharing, retaining existing individual viewing and favorites.
   Typed/spoken reel commands now require review and Save; sends still require
   the existing recipient/preview confirmation.
5. **Phase 5 (implemented):** refine existing native voice entry, transcript,
   cancellation and permission/error UX; no second interpreter or continuous
   listening.
6. **Phase 6:** evaluate optional hosted structured interpretation only after
   deterministic search/actions and permission-aware repositories are reliable.

The earlier schedule/share commands below remain available. Count questions now
use the inline Phase 2 answer cards, not the old count dialogs. New reviewed
result-action controls are described in Phase 4 below.

### Game-day schedule questions

Ask Playloom schedule, roster and unsupported-request answers appear inline in
the same purple answer card as memory questions, not a popup. Each submitted
request replaces the previous answer. Editing or clearing the search dismisses
the action answer; prior memory results remain available underneath. Long
answers scroll inside the card so the library stays accessible.

Answers omit source introductions such as "From your saved schedule" and
"Based on saved roster details". Schedule answers start with the game details.
Missing-data notices and recorded-statistics limitations remain visible.
No-match answers are concise, for example "No upcoming scheduled game against
Penn Trafford." They do not repeat the question or append generic guidance.
Past schedule questions such as "When was the last time we played Mount
Lebanon?" and "Where was our last game against Mount Lebanon?" return the most
recent past scheduled meeting per matching team. Date windows such as "What
games did we play last week?" include all matching scheduled games in that
window. These are schedule records, not confirmation a game took place or a
saved final result. Team scope, opponent aliases and follow-up questions apply.

Ask Playloom accepts "Do we play today?", "When is our next game?",
"Where is our next game?", "What games do we have this weekend?", and
"When do we play Penn Trafford again?" through either typing or the microphone.
The selected library team scopes schedule answers; without a selected team,
answers search all saved teams. Memory player, opponent and season filters do
not restrict schedule answers, and the previous memory search is preserved.

Date-specific questions include all scheduled games within the device-local
calendar dates, including games earlier today. Undated questions use upcoming
games; "next" and "again" select each matching team's next meeting. Weekend
means Saturday and Sunday of the current calendar week. Answers include saved
team, opponent, date, time and location; missing locations are stated explicitly.
They do not infer cancellations, results, travel times, or home/away status.

After an answer containing exactly one game, ask "Where is it?"
or "When is it?" to reuse that game. Multiple matches require a team/opponent
instead of guessing. Changing team scope, leaving the library, submitting a
different kind of request, or a schedule change invalidates that context.
Follow-up context is temporary and is not saved across app launches.

### Phase 2: recorded-memory questions

"Who did Tyson have his first goal against this season?" resolves the player,
season and first recorded goal before returning that memory's opponent and date.
Missing opponent or capture-date metadata is reported, not filled from schedule.
"Where and when was Tyson's first goal this season?" (also "when", "where",
and "when and where") uses that same earliest matching memory. iPhone returns
the capture date and linked game's saved location; Android restored clips have
no game-location link and explicitly show "Location not saved". An opponent
name is not substituted for a venue.

Typed and spoken questions use the same shared `MemoryQuestion` interpretation.
They preserve the question text, show an answer card immediately above the
existing result chips/grid, and recompute from the displayed matching records:

- "How many faceoff wins did Tyson have yesterday?" → "6 recorded faceoff wins"
  only if six saved memories actually match the player, date and tag.
- "How many goals did I record this season?" / "How many favorite memories do I
  have from 2026?" combine the existing season, favorite and date constraints.
  "I" refers to the available local library, not a newly inferred owner identity.
- "Which teams did Tyson score against?" summarizes saved opponent names from
  matching assigned/tagged memories, not scheduled fixtures. Missing opponents
  are explicitly excluded from that list; up to five names are shown. Dates may
  follow "against": "Which teams did Tyson score against yesterday?" and
  "Which teams did Tyson score against this season?" remain memory questions,
  not schedule commands.
- "What was Tyson's first recorded goal of 2026?" shows the earliest matching
  saved memory and its local capture date. Tap its existing card to view it.
- "What was Tyson's first goal this season?" uses the selected season in the
  library. With All seasons selected it uses the current calendar-year season,
  including before August. The interpretation chip shows the resolved year,
  for example "This season (2026)"; the selected season is not silently cleared.

Memories without an assigned season default to their known recording year for
season searches and filters. Edit memory displays that default year and saves it
when details are saved. New phone captures with a selected team persist a season
for the recording year if no season/game was selected. Explicit seasons are
never overwritten; memories with no known date are not assigned a guessed year.

Counts represent matching saved memory records, not deduplicated sports events:
two recordings of the same play remain two recorded matches. They are not
complete game statistics. Empty results explain missing recordings/tags rather
than asserting that a player had no real-world events. On Reels, answers count
saved reels, never individual plays. Existing visible library filters still
apply; removing interpretation chips updates both answer and supporting grid.
Clarification/source-metadata errors suppress the factual answer. No provider,
network request, new storage, or video reanalysis is involved.

Device checks: compare the count to the displayed clips, remove the player/date
chip and confirm both update, try a zero-match request and an ambiguous player,
ask for recorded opponents and the first recorded goal, repeat by voice and
offline, and verify Reels counts cannot be mistaken for event statistics.
`MemoryQuestionTest` covers these shared interpretation/answer rules.

### Phase 3: temporary Play All

Search results on Memories offer **Play all (N)** when at least one matching
original video is available. The playlist snapshots the currently filtered
results, removes duplicates, and plays oldest-first/newest-last. Photos and saved
reels are excluded, so N may be smaller than the total memory count. A single
matching video is supported. Nothing is exported, uploaded, or saved as a reel.

iPhone uses the existing native video player, advances when each video finishes,
and offers Previous, Next, Restart and Replay all. Playback errors remain visible;
Next lets the user move past an unavailable clip rather than silently skipping it.
Android uses the existing Media3 playlist with previous/next and end replay,
without the saved-reel intro. Sharing from Android's existing playback controls
shares the current clip, not an automatically exported reel.

Back returns to the same search, chips and filters and discards the temporary
playlist. iPhone pauses when the app loses focus. Reels remain on their existing
playback path. Reviewed Create Reel/Share result actions are described in Phase 4.

Device checks: search for several videos, tap Play all, verify chronological
automatic advancement, previous/next, pause/scrub, replay at the end, and Back
preserving the query. Repeat with one video, mixed photos/videos, no videos,
rotation, an unavailable file and app background/foreground. Confirm no new reel
appears in Reels after playback. `MemoryPlaylistTest` covers membership/order.

### Phase 4: reviewed result actions

Ask Playloom results use a horizontally scrollable action row on both platforms:

- **Play all (N)** retains the Phase 3 temporary playlist.
- **Create reel** opens the existing Edit Reel screen with the matching original
  videos in chronological order. Review/rename the title, remove or reorder
  clips, or use Add Memories before pressing **Save**. At least two videos are
  required; photos and saved reels cannot become source clips. Opening or
  canceling the draft exports nothing. Back from a result draft returns to the
  search; Back from Add Memories follows the existing picker flow.
- **Share (N)** starts existing multi-selection with the visible results
  preselected. Deselect anything unwanted, then press Share to open the existing
  preview/recipient flow. No send is triggered by the result button. Search/filter
  changes remove hidden share selections; iPhone supports photos/videos and
  Android retains its video library. Reels use their existing sharing flow.
- Tap an individual card to view it, or use its existing favorite star. These
  controls remain available; this phase does not add implicit bulk favorites.

**Intentional behavior change:** typed/spoken "Create a reel..." and "Share a
reel...with Jenna" requests no longer export automatically. They interpret saved
assignments, tags, opponents and seasons with the same structured metadata
matcher as search (never background player detections), then open Edit Reel.
For a named-recipient request, Save creates the reviewed reel and only then opens
sharing with the requested recipient resolved by the existing confirmation UI.
The recipient request survives Add Memories; canceling the draft cancels it.
Canceling sharing after Save keeps the saved reel, but sends nothing.

No cloud interpretation, upload, or new persistence model is added. Source
memories and their tags are unchanged by drafting, reordering or canceling.
`ReelActionReviewTest` covers source interpretation, chronological video-only
membership, chip changes, ambiguity and separating recipient names from criteria.

Device checks: search for several goals, create a reel, rename/remove/reorder a
clip and cancel; confirm Reels has no new item. Repeat and Save, checking the
reviewed order/title. Try a named sharing command, add a memory, save, verify the
recipient and cancel before sending. Use Share on results, deselect one, and
verify preview contains only the reviewed selection. Retry one-video/empty/photo
results, ambiguous player/contact names, rotation, and the existing card stars
and single-memory viewer.

### Phase 5: one-shot voice refinement

iPhone's existing listening dialog now shows the evolving transcript, without
executing partial words. Pause after speaking to submit the completed request
through the same handler as keyboard Search. Cancel discards speech and preserves
the previous query; leaving the library cancels its recognition job. Backgrounding
after listening starts stops recognition and discards the partial request.
Permission continuations ignore late callbacks after cancellation, and a canceled
permission request cannot proceed into microphone recording. The native audio
session is restored through the existing cleanup path.

No-speech and recognition errors now appear beside Ask Playloom with **Retry
voice** and **Dismiss**, rather than being confused with sharing errors or silently
doing nothing. Permission messages identify Speech Recognition or Microphone and
direct users to Settings. Typed requests remain available. Mic and Clear have
larger touch targets; iPhone disables editing while the listening dialog is open.

iPhone requests on-device recognition when the locale/device reports support.
Otherwise Apple's existing system recognition may require a connection. Android
retains the system recognition activity and its permission/listening/cancel UI,
prefers offline recognition where supported, reports empty results and launch/
permission failures inline, and preserves the query on cancel. Android providers
may ignore the offline preference; offline voice is not guaranteed. Metadata
interpretation remains local, and this adds no hosted AI service or media upload.

Physical-device checks: allow/deny microphone and speech permissions; verify
Settings guidance, Retry and typing; say a search/count/reel command and compare
with typed results. On iPhone watch partial text, cancel halfway, retry rapidly,
stay silent, background mid-sentence, then verify no partial action occurred.
Afterward play a clip with audio and return to Camera to check audio restoration.
Try offline with and without downloaded speech-language support. Native device
permissions, recognition quality, and audio-session behavior cannot be confirmed
by the shared regression suite or unsigned builds.

### Ask Playloom roster questions

The Memories/Reels microphone and keyboard Search also answer roster questions:
"Who is number 28?", "Who is wearing jersey number twenty-eight?",
"Who is Tyson?", "What number does Tyson wear?", or "What is Tyson's number?"
Group questions support count variants such as "How many goalies do we have?",
"How many seniors are there?", and "Count our goalkeepers". Roster group counts
bypass the memory counter and show the total saved active matches alongside
the matching players.

Group questions support combined class/position questions such as
"Who is our freshman goalie?"
or "How many senior defenders do we have?" Combined questions require both
saved attributes to match; missing class years are not inferred.
Other examples include "Who are our goalies?", "Who are the seniors?",
"Which players are juniors?", "Show me our defenders", and "Who are the goalies
on Tigers?" Supported groups cover goalie/goalkeeper, defense, midfield, attack,
LSM, FOGO/faceoff specialists, and freshman/sophomore/junior/senior. Saved position
abbreviations and slash-separated positions are supported. Group lookup only
uses saved position/class labels; numeric graduation years are not converted to
class years. Missing details do not count as matches. Results show up to ten
players, the total match count, and an explicit remaining count when truncated.
Results appear in the inline **Roster lookup** answer card without clearing the previous library
search or changing tabs. They show saved name, jersey number, team, and position/
academic year when present; no game participation or biography is inferred.

The selected library team scopes lookup. With All Teams/no selected team, the
lookup searches available saved rosters across teams. The memory player, opponent
and season filters do not hide roster members. Inactive players are excluded.
Duplicate numbers/names are listed with team labels rather than guessing. A full
name or "Who is number 28 on Tigers?" can narrow results; an explicit team in a
question cannot broaden access beyond the selected team. No-match and empty
roster messages suggest using a full name/number or adding/importing in Teams.

Typed and spoken lookup reuse the camera roster's shared lookup helper; this
path does not run camera capture/team-switch commands or approximate-name guesses.
Spoken jersey numbers zero through ninety-nine, including "double zero", are
supported. Lookup uses local saved rosters, not video detections, media analysis,
phone contacts, or a hosted AI service. The answer is displayed, not spoken aloud.
The Camera roster drawer microphone uses the same roster-question parser and
answer formatter for names/numbers, positions, class years, combined filters and
counts. It displays and speaks the answer, scoped to the team chosen in Game
setup; without a camera team it asks you to select one.
The drawer's player answers omit team names and the saved-roster-match
introduction; count questions retain a plain player total. Ask Playloom library
answers still include team labels for cross-team identification.
Drawer answers list every matching player without the library's ten-player cap;
scroll to read the full list. Answers with more than two matching players speak only the total and
direct you to the displayed list; one or two players are still read aloud.
Bare names/numbers retain the existing quick lookup/details behavior, and camera capture/team-switch
commands are unchanged. Long answers remain readable in the scrollable drawer
on iPhone and a dismissible scrollable roster-answer dialog on Android.

Device checks: ask by name and number, select a team, then repeat in All Teams
with duplicate numbers. Confirm team labels, empty-roster/no-match guidance,
typing/voice parity, and that closing the answer restores the prior search.
`RosterQuestionTest` and `VoiceCommandProcessorTest` cover routing, scoping,
ambiguity, number words and existing camera lookup behavior.

### Verify Phase 1 on a device

1. Run the app, open Memories, tap Ask Playloom, and choose a suggestion.
2. Try "Show Tyson's favorite goals from 2025" using names/tags you have saved.
   Check the player, Goal, Favorites and calendar-date chips against the cards.
3. Remove Favorites or the date chip: results should broaden without modifying
   any saved memory. Clear restores ordinary browsing and existing filters.
4. Try "Find Tyson's first goal last season", "Find photos last weekend", and
   "Find goals against Mount Lebanon". Use your saved names/opponents.
5. Use two children and ambiguous name prefixes; a name must not be guessed.
   Try a player who is only detected, not assigned: that must not count as an
   assigned-player match. Explicit detection/source requests explain the gap.
6. Dictate the same retrieval query; verify the same chips and results. Retry
   offline; no AI/network request is needed for metadata search.
7. Verify portrait/landscape, empty results, chip scrolling, existing filters,
   ordinary card playback/editing/sharing, and current-tab search isolation.

Tests: `AskPlayloomQueryTest` plus existing parser, library, date/action and
reel-search regressions. Native UI, microphone permissions, and unavailable
device media still need physical-device verification.

## Existing action-bar questions and reel requests

The on-device action bar on Android and iPhone now recognizes these additional
typed (keyboard Search/Return) and spoken requests:

- "How many faceoff wins did Tyson have yesterday?"
- "Count Tyson's favorite goals this month"
- "Who is our next opponent?"
- "When do we play Mount Lebanon?"
- "Where is our next game?" / "What time do we play Penn Trafford?"
- "Share a reel of tysons faceoff wins with Jenna"
- "Send a reel of the latest 3 goals against Mt. Lebanon to Jenna Smith"

Counts describe **matching saved memories**, not official or complete statistics:
unrecorded plays and missing/incorrect tags cannot be inferred. Counts stay on
the current Memories/Reels tab, and reel counts do not count individual plays.
Questions and reel requests reuse the existing date, player, action, favorite
and opponent filters. Dictated possessives such as "tysons" are recognized from
saved player names; ambiguous first names require a full name.

Schedule answers read saved games, not cloud/live fixtures. They show team,
opponent, date, local time/time zone and location (or explicitly say no location
is saved). Only upcoming games are returned. With no specific team context, each
team's next game is labeled rather than guessing which team "our" means. Android
uses the selected team (or all subscribed teams in All Teams); iPhone uses its
team filter when present. Missing schedules/opponents produce an explicit
no-match answer. Only five matching fixtures are shown at once.

Share commands open a **new** chronological reel draft for review. After an
explicit Save they open the existing share screen for preview and recipient
confirmation; nothing is sent automatically.
Use "of/from <memory criteria> with/to <recipient name>". At least two saved
videos are required. A unique saved contact name is preselected; ambiguous
matches require a selection, and missing contacts can be chosen with Phone
contacts (including native permission/picker behavior). Name matching does not
look up or upload the phone's contacts to an AI service. Canceling sharing keeps
the saved reel. Unsupported questions show supported examples instead of
inventing an answer. This is structured on-device interpretation, not a general
AI assistant; requests and media are not uploaded for interpretation.

Tests: `LibraryActionRequestTest`, `ActionBarRequestsTest`,
`CaptureFavoritesAndNotesTest` and existing guardian/reel tests. Device checks:
verify the four example intents above, absent/ambiguous players and contacts,
missing tags/schedules, sharing cancellation, and actual native recipient/send
confirmation on both platforms.

New reels default to capture-time order, oldest first and newest last, on Android
and iPhone. This applies to action-bar requests and manual selection, including
requests for the "latest N" clips: selection chooses the latest N, then playback
orders that selection chronologically. Existing reel orders and manual editor
reordering are preserved; newly added clips append in chronological order.
Unknown iPhone timestamps use the linked game's start time when available,
otherwise remain last in their original relative order.

Memory thumbnails now use a trash icon instead of a three-dot menu; tapping it
opens the existing removal confirmation. **Edit memory** is in the full-screen
viewer toolbar: videos open the existing editor and return to the viewer, while
photos open their details editor. Reel thumbnail menus remain unchanged.

“Create reel of all videos from today” now uses the capture's local calendar date
instead of searching metadata for the word “today”; “yesterday” is also supported.
The Android search parser carries the same relative-date constraint and filters
using the clip capture timestamp. iPhone uses the capture filename timestamp with
the existing linked-game-date fallback for older undated files.
“Create reel of all plays against Pine Richland” includes untagged videos with that
saved opponent. Spacing/hyphens and unambiguous initials such as PR are supported
in the shared iPhone library search. Opponent words after against/vs/versus are
not interpreted as moment tags. Date and opponent constraints intersect.
Existing reels are excluded from reel sources, and at least two matching videos
are still required. No-match feedback points to capture dates and saved opponent
details; no opponent is inferred from current Game setup for an old unassigned clip.
Typed commands (Return) and spoken commands use the same iPhone matcher; searches
continue to stay on their current Memories/Reels tab.
Tests: `ReelActionSearchTest`, plus existing capture/moment/reel search tests.

The on-device action bar also supports combinations rather than fixed example
sentences: today/yesterday; this/current or last/previous week, month and year;
last/past N days (including today); ISO dates such as 2026-10-26 and month-name
dates such as October 26, 2026 or Oct 26th (current year if omitted).
Weeks run Monday-Sunday. First/earliest and last/latest/newest select by capture
date/time, with optional numeric counts (for example "latest 3 goals").
Favorites/favourites/starred constrain to the user's saved stars; photo/picture
and video requests constrain media type. Player names, jersey numbers, opponents
and action tags combine with those constraints. "Make", "create", "build",
"generate" and "compile" reel requests use the same parser; sources are still
videos only and at least two are required. Examples:

- "Could you show me Tyson's favorite goals against Penn Trafford this week?"
- "Create a reel of the latest 3 saves against PR"
- "Find pictures taken on October 26"
- "Generate a reel of all plays from the past 7 days"

Relative season searches resolve to calendar-year season labels, not an assumed
August rollover. Ask Playloom's "this season" uses the selected library season
when present; otherwise it uses the current year. "Last season" uses the previous
calendar year. Android's existing saved August–July season groups are not
rewritten: an explicitly selected group still uses its saved starting-year key.
On iPhone, searches scope saved season records when present; the legacy
no-season-data behavior is retained outside structured Ask Playloom searches.
Unknown dates do not match a requested date window. A
single undated result can still be returned for "latest" as before, but multiple
undated clips cannot be ranked reliably. Unrecognized words remain search
constraints, not silently ignored filters. This is not arbitrary AI-language
understanding: OR/exclusions, arbitrary date intervals, and number words such as
"three" are not implemented. No request text or media is sent to an AI service.
`ActionBarRequestsTest` exercises varied combined requests and calendar boundaries.
Opponent search also treats the whole-word spellings **Mt.**, **Mt** and **Mount**
as aliases in either direction (for example Mount Lebanon matches saved Mt. Lebanon).
This applies to ordinary searches and reel requests on both platforms without
renaming saved opponents or matching unrelated opponents such as Mount Pleasant.

## Playloom account testing (Android and iPhone)

**More > Account** now offers email one-time-code sign-in, resend, verification,
session restoration and **Sign out on this device**. It is optional; the existing
onboarding, camera, Teams, Contacts, Memories, Reels and Settings remain available.
This is the authentication milestone only: teams/rosters still use the existing
local stores and local test identities. Signing in does not claim, merge or upload
existing teams, contacts, notes or media. No database schema or Storage bucket is
created by the app.

Both hosts use the Supabase project configured in
[`PlayloomCloud`](../shared/src/commonMain/kotlin/com/playerid/app/platform/SupabaseEmailAuthService.kt).
Its publishable key is client-safe, not a database password or service-role key.
Only `/auth/v1/otp`, `/auth/v1/verify` and the refresh-token endpoint are used.
Ktor 2.3.7 is pinned to work with the existing Kotlin 1.9.20 toolchain; this small
Auth REST adapter avoids requiring a Kotlin/Compose upgrade for a newer Supabase
SDK. No HTTP body/token logging is installed.

### Required Supabase dashboard setup

1. Enable the Email provider and allow new signups for testing. The public Auth
   settings were checked on October 23, 2026: both were enabled.
2. Under **Authentication > Email Templates**, make both **Confirm signup** and
   **Magic Link** emails display the code, for example
   `<p>Your Playloom verification code is: {{ .Token }}</p>`.
   The same request handles first-time and returning users; do not show only
   `{{ .ConfirmationURL }}`, since this app expects a numeric code, not a link.
   No deep-link callback is needed for this code-entry flow.
3. For testers outside the project's allowed built-in mail recipients, configure
   custom SMTP and suitable sending/rate limits. Do not disable confirmation to
   work around email delivery failures.

Never paste a service-role key, `sb_secret_` key or database password into either
mobile app. This change does not alter your dashboard settings or send a test email.

### Device checks

1. Open **More > Account**, enter a test email and tap **Send code**.
2. Enter the emailed code and tap **Verify and sign in**. Check an invalid/expired
   code as well; failures should be visible, not dismiss the page.
3. Close/reopen the app and check that the same account is restored.
4. Use the same email on Android and iPhone; the account should match, but team data
   will not synchronize until the next milestone.
5. Sign out on one device. Its local teams/media should remain untouched, and the
   other device remains signed in. This removes this device's stored credentials;
   it is not a server-wide session revocation or account deletion.

Android sessions are AES-GCM encrypted with an Android Keystore key and stored in
the no-backup directory. iPhone sessions use a non-synchronizing Keychain item with
`WhenUnlockedThisDeviceOnly` accessibility. Session writes complete before the UI
publishes a successful sign-in; expired sessions refresh, preserving rotating
refresh tokens. Storage/network failures remain visible with retry/sign-out actions.
These are account credentials only, not a new storage location for family contacts.

Shared tests cover OTP payloads, invalid codes, rate limits, secure-storage failures,
restoration, refresh rotation, cancellation and sign-out. Native Keychain/Keystore
behavior and actual email delivery still require physical-device testing.

## VS Code and physical iPhone workflow

The development targets are Android and iPhone; the Mac is the development machine. Keep shared Kotlin logic and Compose screens in `shared`, and add native iOS adapters instead of duplicating Android features in SwiftUI.

1. In VS Code, choose **File > Open Folder** and select the `Player-ID-Android` repository folder.
2. Choose **Terminal > Run Task** to use the local PlayerID tasks:
  - **Check shared platform boundaries**: fast import-boundary validation.
  - **Validate Android compatibility**: shared unit tests and Android Kotlin compilation, not a full APK build. Requires an Android SDK.
  - **Build shared framework for physical iPhone**: ARM64 device framework only, not simulator code or a signed app.
  - **Open Xcode for iPhone run and debug**: opens the existing native host.
3. Follow **Run on a device** below to sign and launch the app on your phone.

These tasks live in the locally ignored `.vscode/tasks.json`; they are not distributed through Git. Equivalent commands can be typed in **VS Code > Terminal > New Terminal** from the repository root (macOS Terminal works too):

```bash
./gradlew :shared:checkCommonMainBoundaries --console=plain
./gradlew :shared:linkDebugFrameworkIosArm64 --console=plain
open -a Xcode iosApp/iosApp.xcodeproj
```

After changes that affect Android callers, also run:

```bash
./gradlew :shared:testDebugUnitTest :shared:checkCommonMainBoundaries :app:compileDebugKotlin --console=plain
```

VS Code is the primary editor, not a replacement for Apple's SDKs. Xcode provides device signing and Swift/native debugging; Kotlin source-level debugging requires additional tooling. The Xcode Run action builds the device framework automatically, so a separate framework build is only needed for targeted validation.

### Android SDK on the Mac

If Android validation reports **SDK location not found**, configure the Android SDK before retrying; this error occurs before source compilation.

1. Install Android Studio from Google's official Android developer site and complete its setup wizard. VS Code can remain your primary editor.
2. In Android Studio, open **Tools > SDK Manager** (or **More Actions > SDK Manager** from the welcome screen).
3. Install **Android SDK Platform 34**, **Android SDK Build-Tools 34.0.0**, and **Android SDK Platform-Tools**. Review and accept Google's SDK licenses in the installer.
4. Note the **Android SDK Location** displayed in SDK Manager. Create the repository-root `local.properties` file in VS Code with the actual path, for example:

  ```properties
  sdk.dir=/Users/ryanknapp/Library/Android/sdk
  ```

5. Rerun **PlayerID: Validate Android compatibility** from **Terminal > Run Task**.

`local.properties` is ignored by Git. Do not commit machine-specific SDK paths or install an emulator just to compile Android.

## Run in the simulator

1. On macOS, install the current Xcode command-line tools and JDK 17.
2. In VS Code, choose **Terminal > New Terminal**. Run the following from the repository root (macOS Terminal works too):

  ```bash
  ./gradlew --version
  ./gradlew :shared:checkCommonMainBoundaries --console=plain
  open -a Xcode iosApp/iosApp.xcodeproj
  ```

3. In Xcode's top toolbar, select the `PlayerID` scheme and an available iPhone simulator, not **Any iOS Device** or **My Mac**.
4. Choose **Product > Run** (Command-R). The Xcode build phase invokes `:shared:embedAndSignAppleFrameworkForXcode` automatically. No development team is needed for a simulator.
5. To open the simulator window separately, choose **Xcode > Open Developer Tool > Simulator**.

There is no separate CocoaPods workspace to open. The iOS entry point now displays the shared team workspace, not the earlier **PlayerID / Shared application framework** placeholder. This phase implements local team, roster, and schedule workflows, not the full Android feature set.

If the Simulator window cannot open, verify that the full Xcode installation includes `Contents/Developer/Applications/Simulator.app`. An installed iOS runtime and successful `simctl` commands do not by themselves prove that the graphical Simulator app is present. Restore the full Xcode installation through Apple's distribution before relying on graphical simulator runs; preserve this repository and local changes.

Command-line validation does not require signing:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme PlayerID \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  CODE_SIGNING_ALLOWED=NO \
  build
```

## Run on a device

1. Connect your unlocked iPhone with a data-capable USB cable and accept **Trust This Computer** on the phone.
2. In Xcode, choose **Window > Devices and Simulators > Devices** and complete pairing. If Xcode needs a newer iOS SDK to support the phone, update Xcode before building.
3. On the phone, choose **Settings > Privacy & Security > Developer Mode**, enable it, and follow the restart prompts. Pairing may be necessary before this option appears.
4. In Xcode, choose **Settings > Accounts** and add your Apple Account. Enter credentials directly into Xcode, not chat or project files.
5. Select the blue project icon in Xcode's left sidebar, then **TARGETS > PlayerID > Signing & Capabilities**. Enable **Automatically manage signing**, select your **Team**, and change the bundle identifier only if the existing one is unavailable.
6. Select the **PlayerID** scheme and your named iPhone in the top toolbar. Choose **Product > Run** (Command-R).

Keep signing identities and provisioning profiles out of Git. Xcode can change the tracked project when setting a team or bundle identifier; review that diff and keep personal signing values local. Do not use `CODE_SIGNING_ALLOWED=NO` for phone installation.

A free Apple Account generally supports personal-device testing with limited capabilities and periodic reprovisioning. App Store distribution requires paid Apple Developer Program membership.

Before App Store distribution, add the required usage descriptions and entitlements as each native adapter is implemented:

- Camera and microphone: AVFoundation
- Photos library import/export: PhotosUI and PhotoKit
- Speech recognition: Speech
- Contacts: ContactsUI
- Notifications and background processing: UserNotifications and BackgroundTasks
- Subscription purchase and restore: StoreKit

Native service adapters should implement the narrow shared contracts; do not duplicate shared reducers, parsers, policy, or Compose screens in SwiftUI.

General key-value persistence is available through `NSUserDefaultsPreferencesStore`. Referral persistence uses `NSUserDefaultsReferralStorage`, which can be passed directly to `ReferralService`.

`PrivacyInfo.xcprivacy` declares the app-specific UserDefaults required-reason API. Extend that manifest when future native adapters introduce additional required-reason APIs or collect data.

## Team, roster, and schedule workspace

Build and run the `PlayerID` scheme again on your physical iPhone to replace the old placeholder. No simulator is required.

The Android app remains the design reference. Both platforms now use its shared light/dark brand palette and type scale; the iPhone team pages use matching Material icons rather than the interim text symbols. The temporary workspace banner has been removed. The workspace is still local-only as described below.

The iPhone now uses the shared Android app shell and welcome/onboarding layouts. The bottom navigation is **Memories / Camera / More** (Memories is Android's existing label for the Clips route). Teams and Settings are reached through More. After onboarding, the app starts on Camera.

This is **not full camera/media feature parity**. Camera capture works, and Memories now lists and plays those local captures. The complete Android Clips workflow and Settings effects are still being migrated; this capture gallery is an interim slice, not a replacement design.

New empty workspaces show the existing welcome > choice > team search/preview > player selection > ready flow, with Create Team and I'll do this later exits. Team discovery is local-only in this build. Existing workspaces containing teams skip onboarding and keep their data. Completion is saved even when choosing I'll do this later, so subsequent launches go to Camera. Android continues to use its existing onboarding preference and ViewModel effects.

Available workflows:

- Create and select multiple teams; edit sport and home/away jersey colors in team settings. Team names cannot be changed after creation in this version.
- Team creation/settings keeps Cancel and Create/Save above the scrollable form. While editing a new team's name, Done (or the keyboard's Done/check-mark key) finishes name entry and replaces the text field with a name summary and Edit button. The entered name is retained; tap Edit to change it again. This ends the text-input session instead of relying only on a keyboard-hide request. Selecting sport or jersey colors also finishes name entry.
- Add, edit, delete, search, and star roster players. Choose an assigned player from the team overview.
- Paste roster text, review the recognized players, then import. Existing jersey numbers update the existing player. Conflicting duplicate numbers must be corrected before import.
- Add, edit, delete, and search scheduled games. Manual date/time input uses `YYYY-MM-DD` and 24-hour `HH:MM` in the phone's current time zone; start and end must be on the same date.
- Paste schedule CSV or website text, preview recognized games, and import them. Unrecognized rows are not imported. Exact repeat imports are skipped.
- Leave a team without deleting its data, then use **Rejoin local team** to restore it.

Sample roster text:

```text
7 Alex Smith
12 Jordan Lee
```

Sample schedule CSV:

```csv
date,start,end,opponent,location
2026-10-20,18:00,19:30,Tigers,Main Field
2026-10-27,17:00,18:30,Lions,School Gym
```

Data is **on this iPhone only**. This version does not sync with Android, other iPhones, TeamSnap, or a server. Basic camera capture is connected as described below. Invitations, photo import/OCR, speech, billing, and notifications are not implemented in this workspace; unsupported sharing and TeamSnap entry points are hidden.

### Persistence and recovery

The native adapter stores `PlayerID/team-workspace-v1.json` under the app's private Application Support directory. Kotlin owns the versioned schema, repository behavior, import orchestration, and UI; Foundation only reads and atomically writes the file. Android continues to use its existing Room database.

Changes are published only after a successful write. Multi-step team creation and imports use one durable commit. If a read fails, a version is incompatible, or data is inconsistent, the app shows an error and offers Retry rather than replacing the file with an empty workspace. Write failures are reported and leave the current saved data unchanged.

Normal app updates should retain this app data when the bundle identifier stays the same. Deleting the app deletes its local data. Do not uninstall the app or change its bundle identifier to troubleshoot a storage error if you need to preserve data.

### Physical-iPhone acceptance checklist

The framework and unsigned host build checks do not replace this runtime check:

1. Run the updated `PlayerID` scheme on the phone. Existing workspaces should open Camera with the Android-style bottom bar. Open **More > Teams** and confirm your saved teams remain.
2. Create a team with distinct home/away colors. Add two players, edit one, and verify roster search by name and number.
3. Star a player and assign that player from the team overview.
4. Import the sample roster after checking its preview. Confirm matching numbers update instead of creating duplicate players.
5. Add a game, edit its opponent or time, and verify schedule search. Try an invalid date and confirm a visible error, not a saved game.
6. Import the sample schedule twice. Confirm the second import does not add repeat games.
7. Fully close the app from the app switcher, then reopen it without reinstalling. Confirm teams, players, stars, assigned player, and schedules remain.
8. Create a second team and confirm its roster and schedule are separate. Leave the first team, then rejoin it and confirm its data remains.
9. Delete a test player and game, close/reopen, and confirm the deletions persist.

Report any failing step with the exact message or a screenshot. On-device keyboard behavior, layout, and native file persistence must be verified on the phone before calling this phase device-validated.

### App-structure milestone acceptance

Do not uninstall the app or change its bundle ID to perform these checks.

1. Open the updated app. Existing teams should bypass onboarding; Camera should be selected in the bottom bar.
2. Tap the left Memories icon, the center Camera icon, and the right Settings-shaped More icon. Check the icon artwork, selected tint, animation, spacing, and light/dark theme against Android.
3. Open **More > Teams**. Check existing data, select a team, open its roster, and enter a search. Switch to Camera and back to More; the team page and search should be restored. Tap the selected More tab again to return to the More menu.
4. Open Settings from More. Confirm a migration-status message, not nonfunctional settings switches. Return to More.
5. Fully close and reopen the app. Camera should be selected and all saved team data should still be available through More.
6. On a separate fresh test installation, check welcome branding and the Create Team / I'll do this later branches. After choosing I'll do this later, restart and confirm onboarding does not repeat. Do not erase your current installation just to test this branch.

The framework and host build checks cannot prove native keyboard, safe-area, animation, or touch behavior. Report those device results separately before considering this milestone phone-validated.

The app-structure, camera-capture, corrected Memories-gallery, and organization/filter milestones were reported working on the physical iPhone.

### Camera capture increment

The iPhone camera uses an AVFoundation adapter behind the shared camera contract. Photo/Video mode buttons, animated shutter artwork, the team/opponent/player setup pill, and camera roster player rows/details are extracted from the Android screen and consumed by both hosts; Android's CameraX actions remain unchanged. The iPhone page is **not full camera-screen parity**. Game setup supports capture context, the roster drawer below supports browsing/search and explicit player choice for future captures, and preview pinch gestures control native zoom. Rolling, post-capture roster tagging, detection, speech, invites and standby behavior remain pending. Captures appear in Memories and can be viewed or played locally. Scheduled game selection is explicit; automatic current-game matching, editing, export and post-capture tagging remain pending.

- Grant **Camera** and **Microphone** access when prompted. Both are required by this audio-enabled capture configuration. Denial shows instructions for enabling access in iPhone Settings rather than a black, apparently working camera.
- Photo captures save JPEGs; Video captures save QuickTime movies with microphone audio. The same shutter starts and stops recording; switching modes is blocked while recording or saving.
- Narrow displays can scroll the original-sized mode buttons horizontally to reach Video.
- Captures are private app files under `Application Support/PlayerID/media`. They are **not** automatically added to Apple Photos or the shared game-memory index yet. No Photos permission is requested. Deleting the app deletes these files.
- Success is shown only after the native capture completes and the photo write or video move succeeds. Storage and native capture failures are shown explicitly. Timed-out video saves retain their temporary recording instead of deleting it.
- Leaving Camera or backgrounding the app finishes an active recording before stopping the camera. Returning resumes the camera; interruptions offer Resume camera. Background save time is subject to iOS suspension limits, so avoid locking the phone or force-quitting during a save.

#### Physical-iPhone camera acceptance (completed)

The user confirmed the camera increment works on the physical iPhone. Retain the following checks for future camera regressions; do not uninstall the app or change its bundle ID.

1. Run the updated scheme with **Command-R**. Allow Camera/Microphone access and confirm a live back-camera preview, not the old placeholder.
2. Capture a photo in portrait and landscape. Confirm the capture completes and the saved message appears.
3. Scroll the mode row if necessary, choose Video, record 5-10 seconds while speaking, then stop. Confirm the timer stops and the saved message appears.
4. Start another video, switch to More, and return. Confirm recording ends, the camera resumes, and the existing team workspace is unchanged.
5. Background and reopen the app. Confirm the camera releases/resumes without freezing. Repeat while recording; report any save/interruption error.
6. Deny a permission using Settings, then return. Confirm a visible explanation; restore access and tap Resume camera.
7. If Memories does not show a capture, use Xcode's Devices and Simulators window to download this app's container. Inspect `AppData/Library/Application Support/PlayerID/media`: open JPEGs in Preview and movies in QuickTime Player. Verify orientation, duration, and recorded audio. Relaunch without uninstalling and confirm these files remain in a second container download.

Compilation and a saved message alone do not establish preview, file decodability, audio, orientation, or background behavior on a physical phone.

### Memories library increment

The iPhone Memories tab now browses the photos/videos captured in PlayerID's private media directory and opens still images or native AVPlayer playback for videos. This makes iPhone camera output accessible in-app; it is **not yet the full Android Clips library conversion**. The organization increment below adds team/season/opponent/player filters. Android's active Clips experience additionally has imported-video review, player overlays, tagging, trimming/editing, reels, batch operations, teammate review, and sharing/export. Those existing Android workflows remain the source of truth and are not replaced by this capture-only gallery.

Device testing found black thumbnails and nonworking playback in the first gallery build. The corrected implementation generates video thumbnails with AVFoundation (not JPEG decoding), renders grid thumbnails in Compose so native views cannot intercept item taps, and attaches/resizes the native video controller as a child of the host controller. Photos are orientation-normalized for display; thumbnail and playback failures show an error instead of an unexplained black tile. Existing saved files are not modified. The user confirmed the corrected thumbnails and playback work on the physical iPhone. This accepts the capture-gallery slice, not full Android Clips parity. Retain the following checklist for regression checks.

1. Run the updated scheme after taking at least one photo and one video. Open **Memories** and confirm both appear.
2. Check that photo and video tiles contain actual image previews, not only black squares. Tap the center and edge of each tile. Open the photo and verify it displays at full available size with the correct orientation. Return to the grid.
3. Open the video and verify AVPlayer controls appear, the video plays with its recorded audio, and playback can be paused/searched. Return to the grid or switch tabs and confirm audio stops; reopen the video and check it works again. Repeat in landscape.
4. Return to Camera, capture another item, then reopen Memories. Confirm the new file appears without reinstalling.
5. Restart the app and confirm previously captured items remain browseable.

If the existing installation has no saved captures, take a new photo and video; the app does not import the iPhone Photos library in this increment. Photo import, editing, tagging, reel creation, sharing/export, and Android clip metadata remain pending.

### Memories organization and filters (device accepted)

The user confirmed this increment works on the physical iPhone on October 18, 2026. Retain the following checklist for regression checks; this acceptance does not establish full Android Clips parity.

Open a photo/video, tap **Edit details**, choose a team, then optionally one roster player, a season, and a scheduled game. Save persists the assignment in the existing local team snapshot without changing the media file. Choose **Unassigned** under Team and save to remove these details. No player means no specific player association, not tagging every roster member.

The grid uses the original Android filter-label visuals for **Player / Season / All Teams / Opponent**; scroll horizontally to reach all controls. Filters intersect, and the visible/total count shows their result. The Player filter lists names only and the Season filter lists years only (from the season label, else its first game); the same name or year on several teams appears once and matches all of them. Choosing **Unassigned** at the bottom of the Team filter clears the other filters and shows captures without any team assignment. Changing teams clears dependent filters. Switching tabs preserves filter selections; unavailable selections are cleared after roster/schedule changes.

- Previously saved files retain their existing details (or remain unassigned). New captures now inherit the explicitly saved Camera game setup described below; without a selected team they remain unassigned. There is no automatic current-game matching.
- A team-only assignment works without a schedule. Add/import schedules under **More > Teams** to supply seasons and games.
- Organization uses stable filenames rather than absolute iOS container paths. Version-1 snapshots without this optional field remain readable; there is no reset or migration that deletes data.
- Deleting a player clears that player's assignment reference; deleting a game clears only its game reference. Team/season associations and saved media remain intact.
- This is local to this iPhone. It does not synchronize Android Clips metadata, add files to the shared game-memory index, or import Apple Photos.

#### Physical-iPhone organization acceptance

Install with **Command-R** using the existing bundle ID; do not uninstall or reset app data.

1. Open an existing photo and video and confirm their thumbnails/playback still work. Tap **Edit details**, choose a team, and save. Reopen details and verify the selection. Cancel another edit and confirm it does not change the saved details.
2. Assign several captures to different teams, players, and scheduled games. If needed, first add teams/rosters and import schedules through **More > Teams**. Check that a team's menu cannot select another team's player/season or another season's game.
3. Test each Player/Season/Team/Opponent filter and combinations. Check counts and the no-match state. Switch to More and back; filters should remain selected. Use **Clear filters** in the no-match state or the all-options in each menu to reset.
4. Clear one capture's Team to **Unassigned** and save. Confirm **Unassigned** in the grid shows it, but excludes team-assigned captures. With Camera game setup set to No team, capture another item and confirm it starts unassigned.
5. Close and reopen the app. Verify saved details persist, existing teams/rosters/schedules are intact, and original photos/videos still open and play.
6. Delete an assigned player/game under More > Teams. Reopen the affected capture: its team and season should remain, and the removed player/game should no longer appear.

Builds and shared tests do not prove device touch behavior or dialog/video interaction; the user's physical-iPhone confirmation is the acceptance evidence for this increment.

### Camera layout and game setup (device acceptance pending)

The temporary integration banner is replaced with the Android setup pill: the same orange first-use pulse, rounded border, jersey artwork, team/opponent labels, player line, expand icon and offsets. The existing mode row and shutter retain Android's sizes and bottom spacing. The recording indicator uses its red dot/time styling. This is closer alignment, not a claim of pixel-perfect parity on the phone; remaining camera controls and native safe-area/layout differences need separate review.

Tap **Select Team** (or the selected team pill) to open **Game setup**. The sheet follows Android's rounded-bottom-sheet treatment, YOUR TEAM / MATCHUP sections, spacing and jersey swatches. Choose a local team and jersey color (the info icon beside Jersey color explains: "Jersey color helps to find other teammates in the memories"). Type an opponent; chips under the field (shown whenever a team is selected, not only while the field is focused) list every other opponent from the team's schedule (all seasons) and from opponents already saved on memories (up to eight), so a prefilled current opponent doesn't hide the rest; once you type, the chips narrow to matching names (names starting with the typed text first). Edit memory's Opponent field works the same way and also offers all seasons. In both, the field and its chips scroll into view together above the keyboard. Game setup lists the selected team's own opponents first, then opponents from your other teams, so a team without a schedule or tagged memories still gets suggestions. Suggestions appear only after you start typing in Opponent and show names matching the typed text. Tapping one fills the field and hides the list. Tapping a chip fills the field and closes the keyboard. Or tap Use scheduled matchup when a current game is suggested. The Player and schedule section has been removed. The pill's player is always the selected team's My Player (set in Teams) and updates immediately if My Player changes; with no My Player it shows All. New captures and the roster drawer's current-player mark use the same player. Game setup accent colors are now fixed to app theme (they no longer switch to team colors), and the old "New captures use these details. Existing memories are unchanged." helper text has been removed. **Done** saves setup; **Cancel** or tapping outside discards unsaved edits. The explicit season/game and player selectors support persistent association while the rest of the original Android camera workflow is still being migrated.

- Setup persists across tabs and app launches. Jersey color affects the pill only until detection is migrated.
- Every new photo/video inherits the setup present when its shutter/start was pressed. Stopping a video, a native automatic stop, switching tabs or background finalization uses that video's starting details, not a later setup.
- Setup cannot be changed during capture/recording/saving. Earlier saved media is never reassigned by changing Camera setup.
- Choosing **No team (unassigned)** and Done clears the default for subsequent captures.
- Unscheduled opponents are persisted and available in Memories' Opponent filter; **Edit details** can change or clear them. Editing opponent text clears the scheduled-game association to avoid conflicting details.
- Media saves before organization. If writing details fails or a selected player/game was removed during a recording, a visible error says the capture is saved and directs you to **Memories > Edit details**. It does not delete/re-record the media or pretend the details were saved.
- The existing version-1 snapshot gains optional camera setup, jersey label and opponent fields; older snapshots remain readable without resetting data. Android continues using its existing storage, selection callbacks and capture actions.

#### Physical-iPhone layout/setup acceptance

Run the existing PlayerID scheme with **Command-R**, preserving the bundle ID and installed data.

1. Compare Camera with Android: top-left setup pill, jersey artwork, text, dropdown icon, mode row, shutter and recording timer. Test portrait and landscape, long team/opponent names and light/dark mode. Report remaining visual differences; builds do not establish exact visual parity.
2. Open Game setup. Select a team and jersey color (tap the info icon to check its message); enter an opponent. Use the keyboard Done and field Done controls, then sheet Done. Confirm the sheet closes, the keyboard dismisses and the pill updates. Reopen, change values and Cancel; saved setup must remain unchanged.
3. Capture a photo and a short video. Open each in Memories and verify its inherited team/player/opponent details and playback. Verify the Opponent filter finds an unscheduled matchup.
4. Choose a season/scheduled game in setup, capture again, then confirm the season/game details in Memories. During recording, the setup pill must not permit changes.
5. Start a video and switch to More; repeat with backgrounding. Return to Memories and confirm each finalized video plays and retains its starting setup.
6. Change setup to another team, then capture. Check earlier captures keep their old details. Clear setup to No team and verify the next capture is unassigned.
7. Restart the app. Confirm setup and jersey selection persist, and prior teams, rosters, schedules and media are intact. Do not uninstall or reset data.

### Camera roster drawer (device acceptance pending)

The Android-style roster handle is now present above the camera mode row. Tap it or swipe up to open the drawer directly at 75% of the camera height; the collapse arrow or a downward swipe shrinks it to a 35%-height peek, and the expand arrow returns it to 75%. Preview and capture controls move above the drawer, following the Android camera's treatment. The dark panel, drag handle, scrim, row colors, typography, selected-player card and dividers follow Android; player rows/details are shared by both hosts.

- The roster is scoped to the Game setup team and includes active players only. Empty state links to that team under Teams, or the team list if no camera team is selected.
- Tap a player to inspect their position, academic year and team. Browsing alone does not change setup or earlier media.
- The drawer is browse-only: the **Use for new captures** / **Use all players for new captures** buttons and the "Invites and post-capture tagging are not connected yet" note were removed. Choose the capture player in Game setup.
- During recording/saving the roster can be browsed and the voice lookup mic is disabled. The video keeps its original start-time association. While a player selection is being saved, the shutter and setup/mode changes are disabled.
- Player choices are written atomically against the current Camera team. Stale/deleted/inactive players or write failures show an error without changing the previous selection.
- The "New captures: …" status line has been removed; the camera pill still shows the selected player.
- **Microphone (voice lookup):** the search bar has been removed; a mic button with a "Say a name or number" hint sits above the roster in both peek and full heights. It listens for one phrase and matches it with the shared `VoiceCommandProcessor` (the same matcher Android uses). Saying a number ("number 5") opens that player's details card at the top and speaks their name; saying a name speaks their number. As on Android the full roster stays listed below; it is never filtered to the match. Multiple matches are announced in a message line. With no match it says "Sorry no roster match". Tap the red mic to stop listening. Dictation reuses the camera's active audio session (`IosVoiceSearch(shareCameraAudioSession = true)`) so the camera keeps running, and replies use `AVSpeechSynthesizer` (`IosSpeechSynthesis`), which automatically picks the most natural installed voice for the phone's language (Premium, then Enhanced, then the default; novelty voices excluded). To get a natural voice, download one in **Settings > Accessibility > Spoken Content > Voices > English** (e.g. Ava, Zoe, Evan or Nathan — Premium/Enhanced). The mic is disabled while recording/saving. In Rolling mode the spoken reply may be captured in the buffer audio.
- Swipe down from the drawer header to collapse/dismiss, or use the explicit close/expand controls. Invite badges are deliberately not simulated. Post-capture moment tagging is described below. The explicit future-capture action is not Android's existing browse-only row callback.
- Peek (35%)/full (75%) heights are relative to the shared camera's available area (outside bottom navigation), not Android's device configuration. Phone safe areas, keyboard interaction, long text and landscape still require visual acceptance.
- In landscape the roster handle sits directly above the shutter, because the mode buttons are stacked on the right. The Rolling hint moves above the handle.

#### Physical-iPhone roster acceptance

Run **Command-R** with the existing bundle ID; do not uninstall.

1. In Game setup choose a team with a roster. Tap/swipe the handle above the modes. Compare the dark drawer, player rows, details and preview/control movement with Android in portrait and landscape.
2. Expand via the header arrow or swipe up from the header. Search by part of a name and by jersey number. Test a no-match search, keyboard/field Done, collapse, downward swipe and close.
3. Tap a player and check their details, then close the drawer: Camera's selected player must not change. Tap the mic and say a number, then a name; confirm the matching player opens and the reply is spoken.
4. Capture a photo and video; verify their player in Memories. Change the player in Game setup, capture again and confirm earlier captures are unchanged.
5. Start recording, open the drawer and browse. Confirm player actions are disabled and stop remains accessible after collapsing to peek/closing. Verify the saved video's original details and playback. Repeat tab/background finalization.
6. Choose a team with no roster (or clear the camera team). Confirm the explanatory empty state and Teams link work. Return to Camera and verify capture resumes.
7. Restart and verify the last saved player/setup and prior app data persist. Report any touch, keyboard, clipping or layout differences; builds are not device acceptance.

### Camera pinch zoom (device accepted)

The user confirmed corrected pinch zoom works on the physical iPhone on October 19, 2026.

Pinch on the preview to zoom in/out, matching Android's multiplicative gesture behavior without adding a new toolbar or changing the layout. AVFoundation adjusts the actual capture device, so zoom applies to the saved photo/video, not just a scaled preview. The actual device's current minimum/maximum limits are used; no fixed Android zoom range is assumed. Zoom is not persisted as a user preference.

The first implementation's Compose-wrapper gesture did not respond on the physical iPhone. The corrected implementation uses a native `UIPinchGestureRecognizer` on the multitouch-enabled preview, with incremental scale reset after each callback. Shared UI supplies the current zoom-enabled state and error handling. The user confirmed the correction; passing math tests alone did not validate native touch delivery.

Zoom is enabled while ready or recording, but not during startup, photo capture, saving, or Game setup. Drawer/sheet controls retain their own touch handling. Native configuration failures show a dismissible message without stopping an active recording or changing its metadata; dismiss to retry.

Run **Command-R** with the existing installation and check:

1. Pinch in/out in portrait and landscape; verify smooth preview changes and that repeated pinching stops at hardware limits.
2. Capture a zoomed photo and record a video while changing zoom. Open both in Memories; verify saved framing changes, video/audio playback and inherited details.
3. Open Game setup and the roster drawer; verify their menus, search, scrolling and player actions do not zoom the camera. Test zoom on the visible preview with the drawer in peek mode.
4. Switch tabs and background/resume; confirm the camera still captures and pinch zoom works afterward. Zoom may return to the device default when the camera session is reconfigured.

Shared math tests and host builds do not establish physical-device gesture routing, optical framing or recording behavior.

### Recording auto-lock protection (device accepted)

The user confirmed recording auto-lock protection works on the physical iPhone on October 19, 2026. Retain the checks below for regressions.

While a video is actively recording in the foreground, PlayerID disables the iPhone's idle auto-lock timer so an unattended recording does not stop simply because the screen times out. Stopping, saving, a recording error, leaving Camera, or leaving the foreground restores the previous idle-timer setting. Returning to an active recording reapplies the protection. No brightness or system Auto-Lock preference is changed.

This is a scoped iPhone counterpart to Android's camera keep-screen-on behavior, not a new standby mode. Android currently keeps its camera screen awake more broadly; iPhone protection applies only during active foreground recording, not idle preview. The Android standby overlay has no active entry action in the current screen, so no new standby/wake control has been invented. Manual locking, calls and OS interruptions can still interrupt recording, and background finalization remains subject to iOS limits.

On the existing signed installation, run **Command-R** and check:

1. With a finite Auto-Lock interval configured on the phone, start a video and leave the screen untouched beyond that interval. Confirm recording and the screen remain active.
2. Stop recording; verify the clip plays with audio and correct details in Memories. Leave the app idle afterward and confirm normal auto-lock returns (unless another phone setting prevents it).
3. Repeat by leaving Camera during recording and by backgrounding the app. Confirm the clip finalizes and the screen is no longer kept awake by PlayerID.
4. Test manual locking during recording and interruption/resume. Check saved media and any visible errors; auto-lock protection does not bypass the lock button or guarantee background recording.

Physical-device auto-lock/lifecycle behavior cannot be established by framework or host builds.

### Current scheduled matchup suggestion (device acceptance pending)

Game setup now suggests the selected team's current scheduled game using the same shared matching rules as Android: the game starts on today's local calendar date, the current time is from three hours before to four hours after its start (inclusive), and the nearest start wins if several games qualify. Only the selected team's active seasons participate. The suggestion refreshes every minute while setup is open.

Tap **Use scheduled matchup** to populate opponent, season and game, then **Done** to save. This is intentionally a suggestion rather than Android's automatic opponent replacement: iPhone's explicitly saved setup and manual opponent remain unchanged unless accepted. Cancel/outside dismissal discards the draft; no prior captures are modified. No suggestion appears if none qualifies or the suggested game is already selected. Free-text opponents and manual game selection still work.

On the existing installation, run **Command-R**:

1. Import a schedule with a game starting within the matching window today. Select that team in Game setup and confirm the current-game suggestion.
2. Tap **Use scheduled matchup** and verify opponent/season/game populate. Cancel and confirm saved setup is unchanged; repeat and tap Done.
3. Capture a photo/video and confirm its inherited scheduled-game details in Memories. Earlier captures should retain their original details.
4. Switch teams and verify another team's game is never suggested. With no qualifying game, verify manual opponent and scheduled-game controls still work.

This adds no automatic background reassignment or new storage schema. Device interaction and layout still require acceptance.

The user confirmed the scheduled-matchup suggestion on the physical iPhone on October 19, 2026.

**Update (Android parity):** the camera now follows the schedule automatically, so the paragraph above no longer applies. While the camera is open, and Game setup is closed, PlayerID checks every minute. When the selected team has a game in the current window, the camera setup switches to it: opponent, season and game change, while team, player and jersey stay. This uses the shared `cameraOrganizationForCurrentGame`. Each game is applied at most once per app launch, so if you change the matchup it isn't overridden again. **Use scheduled matchup** still appears in Game setup.

The Game setup pill pulses orange on every app launch until Game setup is opened, matching Android's per-session `hasOpenedGameSetup`. Previously it stopped pulsing forever once a setup was saved.

**Setup alert (October 22, 2026):** On iPhone, the pulsing pill is replaced by a larger pulsing red circle with a white **!** until Game setup is opened in this launch. Tapping it shows **Set up your game** (choose team, opponent and player; shows the current team/opponent if one is set) with **Set up game** (opens Game setup) and **Not now**. After Game setup is opened, the normal non-pulsing pill returns. Android's pill is unchanged (`CameraSetupAlertBadge` is used only by the shared iPhone `CameraCapturePage`).

### Memories layout alignment (device acceptance pending)

The capture grid no longer uses the temporary blue My Memories title bar and three-column square tiles. Its surface-colored filter area, shared Android Memories/Reels tab styling, 10dp separator, two columns below 600dp (three at wider widths), 124dp thumbnail cards and 8dp spacing follow the active Android Clips screen. Group headings follow Android's blue vertical accent/underline with team, opponent and season labels. Unassigned files remain visible; groups preserve first appearance and file order within each group.

Reels is visibly unavailable with an explanatory status line. Import, selection/batch actions, sharing, search, tags and editing are not fabricated. The card's top-right action opens the existing working Edit details dialog; tapping the thumbnail still opens the accepted photo/video viewer. This increment aligns the collection surface, not the full Android editor or media feature set.

Run Command-R without uninstalling. Compare the Memories collection in portrait/landscape and light/dark mode; test filters with several teams/seasons/opponents and unassigned files. Confirm every capture remains accessible, actual thumbnails/playback still work, and the top-right details action saves/cancels without opening playback. Verify regrouping after changing details and filter restoration after switching tabs. Exact visual parity still requires phone comparison.

The user confirmed this collection-layout increment on the physical iPhone. This confirms the increment, not full Android feature/visual parity.

### Memories metadata search (device accepted)

The user confirmed metadata search, keyboard behavior and playback on the physical iPhone on October 19, 2026. Retain the checks below for regressions.

The collection now includes a 48dp rounded search bar following Android's search surface, icon, text and padding. Search matches the saved team, player name/jersey number, season, current scheduled opponent/game label, free-text opponent and Photo/Video type. Unassigned captures can be found with "Unassigned". Matching is case-insensitive; all whitespace-separated terms must occur, including across different fields. Existing dropdown filters intersect with search. A blank query restores the filtered list in its original order.

The query survives tab changes. Clear search removes the query without changing filters; the empty-result action clears both search and filters. Keyboard Done and the explicit Done control dismiss the keyboard. Opening media/details dismisses it too. The viewer title is kept alongside its back button instead of consuming a separate weighted area.

This is metadata text search, not Android's natural-language reel/tag/season-intent search or voice search; unsupported speech and reel actions are not simulated. Media files and organization are not modified by searching.

Run Command-R without uninstalling: search a player name/number, team, opponent and season; combine terms and filters; verify the visible count, no-match/reset behavior, tab restoration and keyboard Done. Open a result and confirm full-size photo/video playback and its back navigation still work. Edit details, return and check results reflect the change. Check portrait/landscape and light/dark layout.

### Apple Photos import (device accepted)

The user confirmed the import checklist works on the physical iPhone on October 19, 2026. Retain the checks below for regressions.

In **Memories**, tap **Import from Photos** to choose photos and videos using Apple's native Photos picker. The initial single-item and subsequent multi-selection flows are device accepted. This picker grants access only to chosen items; PlayerID does not request unrestricted Photos-library access. Cancel before selecting leaves the library unchanged. iCloud-only items may take time to download; failures show an explicit message and allow retry.

PlayerID copies the selection into its private Application Support media folder, leaving the Photos original unchanged. Photos are converted to JPEG; Live Photos import as still images, without their motion component. MOV, MP4 and M4V videos are copied without trimming or re-encoding, after checking for a video track. Temporary video copies are staged outside the library and moved into it after copying completes. JPEG writes are atomic. Imported items persist after relaunch and use the same thumbnail, playback, details, search and filter paths as camera captures.

After picking from Photos, PlayerID shows Android's import classification screen. With several items, step 1 asks **Batch classify** (one set of details for all) or **Classify individually** (a card per memory) and previews them; a single item goes straight to the details. Step 2 uses the same Team / Player / Season / Opponent rows as Edit memory (no tag). The team is pre-filled from the Team filter or the only team, and the player/season when there is only one. **Import N Memories** needs a team, season and opponent, plus a player whenever the team has a My Player. Any season year can be chosen (see Edit memory). Cancel (X) imports nothing: the copies made from Photos are removed. Reimporting the same Photos item creates a separate copy. Ordering reflects import time, not the original capture date. Deduplication, source-date metadata and Android/cloud synchronization remain out of scope.

Run **Command-R** with the existing bundle ID; do not uninstall:

1. Cancel the picker and confirm there is no new memory or error.
2. Import a portrait photo (including HEIC if available), then a landscape photo. Check thumbnails and full-size orientation.
3. Import a short video with audio. Check its thumbnail, playback, sound and scrubbing. Repeat with an iCloud-only item if available.
4. Confirm imports appear under Unassigned even when Camera has a team/player/game selected. Use Edit details to organize an imported photo/video; check search and filters.
5. Import while a search/team filter hides unassigned items. Confirm the completion message appears; clear search/filters and find the item.
6. Relaunch the app and verify imported media and saved details persist, older captures still play, and the original items remain in Apple Photos.

Shared format/assignment tests and Android/iPhone builds do not establish native picker, iCloud delivery or imported-file playback acceptance. Those require the physical-iPhone checks above.

#### Multiple Photos selections (device accepted)

On October 20, 2026, the user confirmed import works on the physical iPhone after the multi-selection increment. This confirms the import workflow, not individual verification of all iCloud failure, storage-limit, large-batch or lifecycle scenarios below.

The user selected batch import next, with **no fixed selection limit** and **successful items kept when another item fails**.

- Select multiple photos/videos in the same picker, then add them. PlayerID loads and saves one item at a time, not the entire batch concurrently. Native temporary URLs are copied before their provider callback returns; each copy uses the existing staged-video/atomic-JPEG path. Per-item native autorelease pools limit accumulation of temporary image objects, but a single very large photo/video can still exceed device storage or resource limits.
- The collection shows **Importing completed of total**, followed by the exact success count. A failed item does not stop later selections. **View failed items** lists each failed item's position in the list returned by Photos and its error, including iCloud/download/format/storage failures. These positions are not promises about tap order or chronological Photos order.
- If all items fail, the UI reports failure rather than success/cancellation. Canceling the picker imports nothing. Once submitted, the batch finishes processing its selections; there is no mid-batch cancel control or rollback. Successful private copies are retained even if a later item fails or the app is backgrounded. A batch is not a durable job resumed after process termination; any completed files remain discoverable on reopening Memories.
- Imported copies stay Unassigned and use existing search, filters, details, playback, trim and sharing. Photos originals, existing captures and metadata are unchanged. Retry failed items explicitly via Photos; reselecting already successful items makes additional copies.

Run Command-R without uninstalling:

1. Select several mixed photos/videos, including portrait/landscape and a clip with audio. Verify the completed count and exactly one new copy per successful selection; open every output and check orientation/playback/audio.
2. Cancel an empty picker and verify no files are added. Repeat a single-item import to check the original flow remains functional.
3. If available, select an unavailable iCloud item with local items. Verify later items still import and the successful count plus failed-item count matches the selected total. Open View failed items, check errors, then retry only failed selections.
4. Test a batch larger than 20 items and a large video, checking responsiveness/progress and storage errors. Verify new media remains Unassigned even with Camera setup or a team filter active; clear filters to find it.
5. Relaunch and verify successful imports persist. Share/trim an imported video, and confirm original Apple Photos items and older PlayerID memories remain intact.
6. Background or leave Memories during a batch, then return and check saved outputs. No result is claimed for unfinished items if iOS terminates the app; retry those explicitly.

Shared tests cover empty cancellation, sequential ordering/progress, mixed media, partial/all failures, more than 20 selections, and propagation of unexpected errors/cancellation. Device-only picker/download/large-file behavior remains unverified.

### Viewer layout alignment (device accepted)

The user confirmed this viewer-shell increment works on the physical iPhone on October 19, 2026. Retain the checks below for regressions.

The photo/video viewer now uses the same shared primary-blue, empty-title toolbar as Android's single-memory viewer. Back returns to the existing collection without clearing search/filters. The iPhone uses an **Edit details** action instead of Android's **Edit Clip**, because metadata organization is available but video editing is not. Android retains its existing editing callbacks and review-mode action visibility. Android retains its top-bar system insets; iPhone uses the host's existing safe-area handling.

The interim Photo/Video heading, always-visible metadata row and explanatory footer are removed so the remaining space is a black media canvas. Team/player/season/game details remain available through Edit details. No assignments or media files are changed by this layout update. AVPlayer containment, controls, playback and lifecycle cleanup are unchanged; full-size photos still use aspect-fit display.

This is a viewer-shell alignment increment, not complete Android viewer parity. Android's floating metadata ticket, custom playback controls, viewer zoom, overlays, tagging, trimming, reels and sharing remain separate work. Native AVPlayer controls remain intentionally visible instead of presenting nonworking Android controls.

Run Command-R without uninstalling. Open both camera captures and imported photos/videos; verify the blue toolbar, Back, Edit details save/cancel and the larger black media area in portrait/landscape and light/dark mode. Check video audio, pause/scrubbing and stopping audio when leaving. Return to the collection and verify search/filter state and all saved organization remain unchanged.

### Viewer pinch zoom (device accepted)

The user confirmed viewer pinch zoom on the physical iPhone on October 19, 2026. Retain the following checks for regressions.

The iPhone photo/video viewer now shares Android's tested multiplicative 1x-4x zoom limits. Pinch outward to enlarge the center of the media and inward to return to the original fitted view. Zoom resets when a memory is closed/reopened; it is not an edit and never changes media files, thumbnails or assignments. This follows Android's centered zoom rather than adding pan or gesture-centered repositioning.

Full-size photos use Compose gestures and a clipped display transform; grid thumbnails are unchanged. Videos use a native pinch recognizer on the player host, with a display-only Core Image video composition that scales/crops frames about their center. The native controls and toolbar are not scaled. Paused video replaces its composition to request a refreshed frame without seeking or resuming playback. AVPlayer still owns audio, scrubbing, playback errors and cleanup. Android now calls the same bounds helper while retaining its existing gesture/rendering path.

Run Command-R without uninstalling:

1. Open camera and imported photos in portrait/landscape. Pinch out repeatedly to reach the maximum; pinch in to restore fit. Confirm the image remains inside the black media area and toolbar/Back/Edit details stay usable.
2. Repeat on camera and imported videos while playing and paused. A paused frame should update without starting playback; playing video should keep audio and normal controls. Verify scrubbing and rotation while zoomed.
3. Close/reopen each memory and confirm normal fit is restored. Switch tabs or background the app while playing; audio must still stop as before.
4. Confirm collection thumbnails, search/filters and saved assignments remain unchanged.

Shared unit tests and platform builds verify zoom arithmetic and integration, not physical gesture routing, paused-frame refresh, video smoothness or HDR appearance. Report any black frames, stuttering, brightness/color changes or control conflicts on the phone.

### Viewer metadata ticket (device accepted)

The user confirmed the revised native video ticket and blank-white-screen fix on the physical iPhone on October 19, 2026. Retain the checks below for regressions.

The viewer now reuses Android's shared translucent black metadata ticket: 52% black, white 14sp text, 8dp corners, 12dp horizontal/10dp vertical padding and 14dp top/left placement. Android keeps its tag/opponent/date inputs; long opponents now ellipsize to keep the ticket within its 360dp maximum and the available width.

On iPhone the ticket shows **vs opponent** from the saved organization, preferring the current scheduled-game opponent over a manually saved opponent. No ticket appears without an opponent. Moment tags and original capture dates are not yet stored in the iPhone library, so no substitute tag, import date or scheduled date is fabricated. Team/player/season/game remain available through Edit details.

The photo ticket sits outside the zoom transform. The first video-overlay implementation used a second transparent Compose controller, but device testing reported a blank white screen when opening video. The video adapter now uses a small noninteractive UIKit label/container above AVPlayer instead, with the same opponent text, translucent background, rounded corners, padding and width limit. It has no full-screen rendering surface and is removed when playback closes. This native rendering deviation avoids the nested Compose layer; metadata selection remains shared Kotlin logic. It does not burn metadata into saved media or exports. Video playback and the revised ticket require device rechecking.

Run Command-R without uninstalling. Open camera/imported photos and videos with a scheduled or manual opponent. Check ticket placement, long-name ellipsis, portrait/landscape, and no ticket on unassigned/team-only items. Change the opponent in Edit details and confirm immediate update; Cancel must preserve it. Verify pinch leaves the ticket fixed, video remains visible through the overlay, playback controls/scrubbing still work, and Back/tab changes stop audio as before. Exact native layering/touch behavior requires physical-iPhone confirmation.

### Android-style video controls (device acceptance pending)

The iPhone single-video viewer replaces AVPlayer's standard control chrome with Android-style bottom controls: a white 4dp track with 35%-white remainder, 20dp thumb in a 40dp seek target, 16dp horizontal inset, 12dp gap, and a centered 36dp play/pause icon in a 48dp target, 20dp above the bottom. Controls stay visible and are not scaled by pinch zoom.

The UI is rendered with native UIKit controls inside the existing video host rather than another Compose controller, avoiding the rendering arrangement that caused the white-screen regression. A shared Kotlin playback controller owns pause/resume during scrubbing, clamped seek positions, replay from the end, error reporting and interruption guards. Android's existing ExoPlayer controls and scrub-preview behavior are unchanged.

Tap or drag the seek bar to choose a position. Playing video pauses during the gesture while the main video follows the drag, and resumes only after the final successful seek; paused video remains paused. A canceled gesture leaves playback paused. Seek errors appear above the controls and allow retry. Unknown-duration media hides the seek bar until duration becomes available. Play at the end seeks back to the start. Leaving the screen or losing foreground activity pauses playback and invalidates pending seek completions so they cannot restart audio later. VoiceOver labels identify Play/Pause and Video position; adjustable seek steps are 5%.

The native player supplies video/audio decoding and continuous seeking during dragging. Playlist controls, speed/full-screen/AirPlay menus, editing and sharing remain out of scope.

Run Command-R without uninstalling:

1. Open camera and imported videos. Confirm picture/audio and white seek/play-pause controls, with no native control chrome or blank screen.
2. Pause/play several times, tap different seek positions and drag to the beginning/middle/end. Playing video must resume after release; paused video must stay paused and show the chosen frame.
3. Let a video finish, then press Play to replay it from the start. Check short and longer videos.
4. Test pinch zoom, opponent ticket, portrait/landscape, and Edit details/Back. Controls and ticket must remain fixed and touchable.
5. Leave Memories or background the app while dragging/seeking; confirm audio stops and does not restart after the seek finishes. Reopen and check playback still works.
6. If using VoiceOver, check button labels and adjustable seeking.

Shared state tests and builds cannot verify native slider touch delivery, device seek accuracy, control appearance or playback smoothness. Physical-iPhone acceptance remains pending.

### Main-video live scrubbing (device acceptance pending)

At the user's request, the separate preview box is removed. Dragging the seek bar now seeks the main AVPlayer picture continuously, forward or backward, so the visible clip follows the user's drag rather than playing at a selected fixed rate. Audio remains paused while dragging. Releasing resumes normal playback only if the clip was playing before the drag; otherwise it stays paused on the selected frame.

Shared Kotlin keeps one native seek in flight and retains only the latest requested position. On completion it immediately seeks to that latest position instead of building a queue of old frames. The slider remains enabled throughout dragging. On release the final queued position completes before playback can resume. Cancellation/background/viewer disposal discard queued work and invalidate callbacks so late completions cannot restart audio. Errors on the final seek are visible and leave playback paused. The separate thumbnail generator, preview views and timestamp overlay have been removed; media files, zoom and opponent tickets are unchanged. Android's existing scrub-preview implementation is unchanged.

Run Command-R without uninstalling. On short and long camera/imported videos, drag slowly/quickly in both directions and confirm the main picture follows the slider with no preview box. Release while playing and paused; check the final position and correct resume state. Test end points, canceled drag, rotation, pinch zoom and opponent ticket. Leave/background during a drag or pending seek; audio must not restart. Check high-resolution/imported videos for lag.

Shared tests verify continuous seeking, latest-position coalescing, final-seek resume and interruption behavior; platform builds verify AVFoundation wiring. Actual frame latency and smoothness depend on the video/device decoder and require physical-iPhone testing; builds do not establish frame-perfect tracking of fast drags.

Device testing reported jerky slow dragging. Live seeks now permit a 33ms frame tolerance rather than requiring an exact decode for every intermediate position. Release always queues a zero-tolerance final seek, even if the last live seek already completed; playback waits for that exact position before resuming. Normal-size video also bypasses the Core Image zoom composition until zoom is actually used, and returns to the unprocessed path at 1x. This reduces decoding/processing pressure without adding a preview box or changing saved files. Recheck slow forward/backward dragging, final positioning, paused/playing release and zoom on the physical iPhone; smoothness improvement is not established by compilation.

The user reported continued stutter, including short PlayerID recordings. Seek-completion delivery now uses the main dispatch queue rather than the main operation queue, removing that scheduling layer from the chained seek path during slider tracking. Debug builds emit one `PlayerID scrub:` timing summary after each completed/interrupted gesture: event/seek counts, average/max native seek latency and maximum callback-delivery delay in milliseconds. These logs contain no filenames or media contents; release builds do not collect gesture timings. This is a targeted scheduling change plus diagnostics, not verified smoothness. If stutter remains, copy the summary from Xcode's debug console after slowly dragging a short clip; native seek latency versus callback delay determines the next investigation rather than another tolerance guess.

Scrub smoothness is **deferred and unresolved** at the user's request on October 20, 2026. Do not treat builds or further feature acceptance as acceptance of smooth scrubbing.

### Single and multi-memory sharing (sharing entry points device accepted)

The user confirmed all three sharing entry points work on the physical iPhone on October 20, 2026: thumbnail Share, header Select for multiple memories, and post-recording Share Clip. This confirmation does not separately establish every destination/large-file scenario below or acceptance of tagging/deletion.

- Tap the visible **Share icon** at the bottom-right of a thumbnail. This prepares only that memory for sharing. The separate **Memory actions (...)** menu retains Edit details. The recipient increment below adds a choice between addressed guardian sharing and General share.
- For multiple memories, tap **Select in the top Memories header**, check thumbnails, then tap **Share** beside the selected count. Tapping a tile in Select mode toggles selection instead of opening playback. The selected count shows the exact number to share; Share is disabled at zero selections.
- Cancel exits Select mode and clears selection. Search/filter changes remove hidden selections, as explained in the selection UI; only currently visible selected memories are sent. Canceling/completing the share sheet preserves the current selection for retry. Selections are temporary and are not restored after leaving Memories.

Sharing passes the selected original private media files to `UIActivityViewController`, without merging, trimming, re-encoding or modifying the library. Photos and videos can be shared together. Destinations such as AirDrop, Messages, Save to Files or Save Image/Video depend on the device, installed apps, item types and receiving destination limits. Existing embedded metadata in the files may be included; PlayerID team/player/opponent labels and display-only zoom/tickets are not added to exported files. No automatic upload or cloud synchronization occurs.

Every selected URL must be a readable local file before the sheet opens; failure shows an error instead of silently sharing only a subset. The adapter distinguishes system-reported completion, cancellation and errors. "Share completed" means the system activity reported completion, not verified receipt by a remote recipient. The add-only Photos usage description supports the system Save Image/Video action when offered; the app does not request unrestricted Photos-library access.

Shared Kotlin owns selection and single/batch request construction, while the iOS adapter owns only file checks and native sheet presentation. Android sharing behavior is unchanged. This is system file sharing, not Android's teammate/player-recipient review workflow, edited exports or reels.

Run Command-R with the existing bundle ID; do not uninstall:

1. Tap a photo thumbnail's visible Share icon. Confirm only that photo is offered; cancel and check the library is unchanged. Repeat with a video. Confirm the icon does not open playback or Edit details.
2. Tap Select, check two or more mixed photos/videos, and Share. Confirm the share sheet contains exactly those items. Save to Files or AirDrop, then open the received files and verify full photo/video content and audio.
3. Cancel the sheet and retry. Cancel Select mode, verify checks clear and normal tile taps open playback again. Share must remain disabled with zero selected.
4. Test search/filters while selecting; hidden items must leave the selected count and outgoing request. A thumbnail Share after exiting selection must still send only that thumbnail.
5. Test portrait/landscape, a large video, and a destination supporting multiple files. If Save Image/Video is offered, verify permission handling and saved output without changing originals.
6. Confirm thumbnails, details, imported files and older camera captures remain intact; do not treat known scrub stutter as a new sharing regression.

Builds and selection unit tests cannot verify native sheet contents, transfer/receiving-app behavior, Photos permission or file receipt. Physical-iPhone acceptance remains pending.

### Post-recording "What happened?" screen (Share Clip device accepted)

The user confirmed post-recording sharing on October 20, 2026. Separate acceptance of moment tags, custom-button management and clip deletion has not been recorded.

After a video and its organization details finish saving, Camera shows the shared post-recording screen with Android's **Goal, Save, Faceoff Win, Assist, Defensive Stop, Custom, Share Clip, Delete, and Skip** actions. Photos continue directly to Memories without this prompt. Videos finalized by backgrounding, leaving Camera, or native recording completion remain available for review on returning to Camera during the same app session; the prompt is not a durable queue restored after process termination. The video itself is already durably saved.

- A moment-tag button saves the label and returns to Camera. Custom saves a trimmed label and a reusable custom button. Its menu supports Rename and Remove button without retagging old memories. Blank/duplicate custom names are rejected or reuse an existing button; failures remain visible and do not acknowledge a successful save.
- Tags are stored by filename in defaulted version-1 snapshot fields, independent of team assignment. They survive relaunch/container relocation and appear on thumbnails, the existing photo/native-video ticket, and metadata search. Tags are not burned into exported media.
- Share Clip prepares only the newly recorded video, using the same recipient/general-share flow as thumbnail sharing. Completion returns to Camera; cancellation keeps the post-recording screen available for retry. Detection suggestions and Android/cloud tag synchronization are not connected.
- Delete requires confirmation and removes only this clip from PlayerID's private media folder, then clears its organization/tag. It does not delete other captures or team/roster/custom-button data. Errors remain visible; if file deletion succeeded but metadata cleanup failed, Delete retries cleanup. Skip keeps the saved clip unchanged.
- The review is a shared Compose page rather than another native modal, so the system share sheet can present without conflicting with a Compose dialog. Android's existing camera/post-recording behavior is unchanged.

Run Command-R without uninstalling:

1. Record and stop a short video. Confirm the screen appears only after saving and each named action is visible in portrait/landscape. Skip, then check playback/audio and inherited organization in Memories.
2. Record again and choose Goal. Verify the thumbnail label, viewer ticket and search for `goal`. Relaunch and confirm the tag remains.
3. Create a custom tag, record again and reuse it. Rename/remove its button and confirm prior video tags do not change. Check keyboard dismissal, long labels and scrolling on the phone.
4. Record, choose Share Clip, cancel the system sheet, and retry. Save to Files/AirDrop and open the received video with audio. Confirm only the new clip is sent and the private original remains.
5. Record another disposable clip, cancel Delete confirmation, and verify it remains. Confirm Delete, then check only that clip disappeared while older captures/team data remain.
6. Finalize a recording by switching tabs/backgrounding and return to Camera. Check one review prompt appears, and dismissed prompts do not reopen on tab changes. Confirm photo capture still skips this screen and recording can resume after every action.

Builds/unit tests do not establish post-recording layout, native presentation/transfer, touch handling or deletion acceptance. Scrub smoothness remains deferred and unresolved.

### Guardian/player sharing (adding guardians device confirmed)

On October 20, 2026, the user confirmed the Add guardian from Contacts flow after reviewing the recipient screen and its team/player-link instructions. This records acceptance of adding guardians, not separate verification of every Messages/Mail send, draft, attachment or delivery scenario below.

The user selected this next increment after confirming the three existing sharing entry points, and chose iPhone Contacts plus addressed Messages/Mail drafts. Each sharing entry point now opens the shared **Who are you sharing with?** step:

1. **General share / AirDrop / Save to Files** preserves system file sharing without requiring a team, roster or guardian.
2. Choose the guardian's team. A single assigned team is suggested; mixed-team/unassigned selections can choose explicitly. This team scopes the contact list only: **every originally selected file** is retained, even for a mixed-team batch. No hidden per-recipient filtering occurs.
3. **Add guardian from Contacts** opens Apple's contact picker. Open a contact and choose a phone number or email address. Only the chosen name/address is saved; no address-book enumeration, bulk import, or unrestricted Contacts permission is requested. Add a second endpoint separately if both Messages and Mail are needed.
4. Select one or more active roster players to link to that guardian, then Save guardian links or Continue. Links and the selected contact endpoint are stored in PlayerID's existing local snapshot, not Apple Contacts. A matching phone/email updates that team's contact instead of duplicating it. Choosing a saved guardian allows changing these links; Remove only removes the local guardian record. Player deletion removes its links.
5. A phone endpoint offers **Continue to Messages**; an email endpoint offers **Continue to Mail**. PlayerID opens an addressed native composer containing all selected files and a short editable message. **Nothing sends automatically:** inspect recipient, attachments and message, then tap Send yourself. Guardian links describe the contact's roster relationships, not players detected/featured in the selected clips, and do not change existing memory organization/tags.

Cancellation returns to the recipient step for retry and does not delete media or discard already saved guardian links. Mail's Save Draft is reported as **not sent**, and remains available in Mail; it is not treated as successful sharing. Native Send completion means the composer accepted sending, not confirmed delivery/receipt. Messages/Mail may apply their own attachment/size/format limits or transformations. Mail requires a configured Apple Mail account; Messages requires native text/attachment capability. Unsupported or failed attachment preparation shows an explicit error before presentation rather than silently dropping files or changing to a different sharing method. General share remains the explicit alternative.

This increment deliberately shares UI/rules/storage in Kotlin; Contacts, MessageUI and file checks are native adapters. Android's existing contact/sharing workflow is unchanged. No cloud/contact synchronization, automatic upload, bulk-recipient messaging, message delivery tracking, or multi-player memory reassignment is implemented.

#### Android-style Share screen (awaiting device check)

On October 21, 2026, the user asked to replicate Android's Memories Share screen. It replaces the **Who are you sharing with?** dialog on every iPhone sharing entry point (thumbnail Share, Select > Share, viewer and post-capture **Share Memory**). The screen is full-screen and matches Android's layout:

- **Header:** Close (X) and a centered **Share** title, matching Android. On October 21, 2026, the top-right general share (AirDrop / Save to Files) icon was removed at the user's request.
- **Preview:** A 158dp black preview of the first memory, its colored moment/reel chip, **vs Opponent**, and the game date or **Team memory**. Batches show an **N memories** badge. For videos, a center Play/Pause button plays the clip inline (replaying from the start after it ends), and a top-right full-screen button opens Apple's player with native controls. Inline playback pauses when the app goes to the background or when Messages, Mail or Contacts cover the Share screen. Photos stay a still preview.
- **Suggested players:** Shows the players assigned in the memories' details, because iPhone has no player detection. The section appears only when there are suggested roster players and starts collapsed. The players are still preselected, and the header shows how many are selected.
- **Roster:** Lists active team players. Selecting a player shares with that player's linked guardians. Players without a guardian show **Link**, which opens Apple's contact picker and saves the link.
- **Favorites:** Lists the team's starred contacts (see **Team Contacts** below), with select and unstar (X). Linking a player's guardian here does not make them a favorite. When there are no favorites, the section shows **No favorites yet.** with a **Set favorites** link. It opens the same Team Contacts screen inside the Share screen; Back (or swipe-dismiss) returns to the share with its selections intact, and new favorites appear immediately.
- **Reels:** A reel has no details of its own, so sharing uses its source clips' details (`shareOrganizations` in `GuardianSharing.kt`). A reel built from one team's memories opens Share with that team preselected. It shows the same Favorites (and the source clips' players as suggestions) as sharing those memories. If the clips span several teams, the user picks a team as for a mixed batch.
- **Phone contacts:** Picks one contact for this share only. It is not saved.
- **Share button:** A green **Share with N people** button sends to everyone selected, with duplicates removed. Recipients with a phone number share one Messages draft; email-only recipients get a Mail draft afterwards. If Mail is canceled after Messages sent, retrying Share opens Mail only. Nothing sends automatically.

#### Team Contacts: player contacts and favorites (awaiting device check)

At the user's request, player points of contact (POCs) are now separate from share favorites. Every player can have POCs without filling up Favorites. **Teams > [team] > Contacts** sits under Roster and Schedule. Its subtitle shows "N player contacts · M favorites".

- **Favorites:** Starred contacts appear in the Share screen's Favorites. **Add favorite from phone contacts** opens Apple's contact picker and stars the chosen contact. A favorite doesn't need to be linked to a player (for example, a coach or grandparent). Tap the gold star to unstar.
- **Player contacts:** One card per active roster player. **Add** picks a POC from Contacts; a player can have several, and one contact can be the POC for several players. Each POC row has a star to make it a favorite and an X to unlink it from that player.
- **Rules (shared Kotlin, `LocalTeamRepository`):** `GuardianShareContact.isFavorite` defaults to false, so guardians saved before this change load as POCs, not favorites. The repository functions are `linkShareContact`, `addFavoriteShareContact`, `setShareContactFavorite` and `unlinkShareContact`. A contact is deleted only when it has no linked players and isn't starred; deleting a player prunes the same way. The tests are in `GuardianSharingTest`.
- **Android:** It keeps its own contact flow. Android passes no `onContacts` callback, so the row is hidden there.
- **Streamlined layout (October 22, 2026):** The descriptive text and success messages were removed; errors still show. Each player row reads **Name  #12** in one style, with a chevron. The full name always shows; long names wrap to a second line instead of truncating. Tapping the row expands it to show that player's contacts and an **Add contact** button; tapping again collapses it. All rows start collapsed. Players, favorites and each player's contacts are sorted alphabetically by last name (ignoring Jr./Sr./II–V), then by full name. The favorites button reads **Add favorite from phone contacts** with a contact-card icon, so it is clear it opens the iPhone contact list.

Device check: open Team > Contacts. Add a POC for a player, then confirm it doesn't appear under Share > Favorites. Star it and confirm it appears there. Add a favorite from phone contacts with no player. Relaunch and confirm everything persists.

Recipient resolution is shared Kotlin (`memoryShareRecipients`, `guardianShareRoutes`, `suggestedMemorySharePlayerIds`) with `GuardianShareRecipientsTest`; native MessageUI now accepts multiple recipients.

Run Command-R with the existing bundle ID; do not uninstall:

1. From thumbnail Share, select a team and add a guardian's phone from Contacts. Cancel the picker once, then retry. Link roster players, open Messages and verify the recipient, one attachment and editable text. Cancel and verify the recipient page remains usable.
2. Send a disposable test photo/video to a consenting recipient and verify actual receipt, playback/audio and original private media retention. Reopen sharing and confirm the saved guardian/player links persist after relaunch.
3. Add an email endpoint, open Mail, verify its recipient and all attachments, then test cancel, Save Draft and Send separately. Save Draft must say not sent. If Mail isn't configured, confirm the error and General share alternative work.
4. Repeat from header Select with multiple mixed photos/videos and from post-recording Share Clip. Verify the exact original selection is attached and no unrelated memory appears. For a mixed-team batch, verify the team choice does not silently drop files.
5. Change saved guardian links, remove a local contact and delete a linked roster player. Confirm unrelated contacts, media, roster entries and Apple Contacts remain unchanged.
6. Try General share from each entry point, including with no teams/players, and check cancel/retry, selection retention and AirDrop/Save to Files. Test portrait/landscape and background/resume around native sheets.

Builds and shared tests verify compilation/storage/request rules, not Messages/Mail availability, attachment limits or delivery. Adding guardians is device confirmed; the remaining composer/delivery checks above have not been individually confirmed. Scrub smoothness remains deferred.

### Video trimming and saved-copy export (original-retention choice device accepted)

On October 20, 2026, the user confirmed the post-trim Keep original / Delete original choice works on the physical iPhone. This confirms the retention workflow, not separate acceptance of every codec, export-quality, interruption or failure-recovery scenario below.

The user chose trimming next: save a new memory and copy the original's details/tag. On October 20, 2026, the user refined original retention to an explicit **Keep original / Delete original** choice after each successful trim.

- Open a video in Memories and tap the **scissors / Edit and trim clip** action in the viewer header. Photos have no trim action.
- The shared **Edit & Trim Clip** page follows Android's blue editor header and two-handle Trim range control. Start/end and selected duration are displayed to the millisecond. Crossed, zero-length, out-of-bounds and non-finite ranges cannot be exported. Very short selections can still fail native encoding if they contain no decodable frame; that error is visible.
- Drag the range handles to choose the portion to keep. Preview pauses and seeks to the selected start when the range changes. Play previews only that range; replay returns to its start. The preview's seek control is relative to the selection. This reuses the accepted native player, without adding a second Compose controller over video. Known slow scrub stutter remains deferred.
- **Save copy** exports through AVFoundation's highest-quality preset using MP4 when supported, otherwise MOV. It exports the selected time range with the source audio/orientation; no display zoom, tickets, player bubbles or overlays are burned in. This is a re-encoded edited copy, not a promise of byte-identical quality, original codec/HDR/color properties, or arbitrary sub-frame cut precision.
- The output is first written to a unique temporary file, checked for playable video/positive duration, then copied through existing staged private-media import. Only the successful copy becomes a memory. The source is never overwritten. Export failures and cancellations are explicit; temporary-file cleanup failures are logged.
- Team, player, season, game/opponent and moment tag are copied together in one shared snapshot write. Unassigned originals remain unassigned, even if Camera currently has a team selected. If this write fails after the media is saved, the page explicitly says the copy exists and **Retry details** retries metadata only, without exporting a duplicate. Leaving then keeps the playable new copy, potentially without details.
- After the trimmed file and copied details are successfully saved, **Keep the original clip?** offers **Keep original** or **Delete original**. Keep retains both memories. Delete permanently removes only the source from PlayerID and clears its organization/tag, leaving the trimmed copy and unrelated data intact. The dialog explains the deletion and does not dismiss on an outside tap. Both choices open the new copy; use its thumbnail Share icon or Select to share. No original is deleted automatically if the editor is left before choosing.
- If deletion fails, the trimmed copy stays saved and the error allows retry or Keep original. If file deletion succeeds but metadata cleanup fails, Keep is disabled (the file is already gone) and Delete retries cleanup without exporting again. No deletion choice is offered after export or detail-copy failure.
- Back before Save discards only the draft range. Cancel export requests cancellation while encoding; once file finalization/metadata saving has begun, a completed save is retained. Backgrounding cancels an active encode. Switching tabs does not discard a successful in-flight save. Range drafts are not persisted after leaving the editor.

This is the trim/export slice, not Android's full editor: name bubbles, overlay placement, detection, crop/rotation tools, reels and edited Photos-library replacement remain out of scope. Android's editor/capture/sharing behavior is unchanged.

Run Command-R with the existing installation; do not uninstall:

1. Record/import a video with recognizable movement and audio. Open it, tap scissors, choose known start/end times (for example 2.000 to 8.000 seconds), then Play. Verify the preview starts/stops at those boundaries and replay/seek stay within them.
2. Save copy and choose Keep original. Verify a separate approximately six-second memory opens, with the expected first/last frames, audio sync and orientation. Frame/sample boundaries can differ slightly from the requested millisecond timestamps. Confirm the original's full duration/content remain unchanged. Repeat with a disposable source and choose Delete original: only that source should disappear, while the trimmed copy retains playback/details after relaunch.
3. Verify both copies' team/player/season/game/opponent/tag, search results and filters. Test an unassigned imported video while Camera has a team selected. Relaunch and verify both media and copied details remain.
4. Share only the trimmed copy through guardian Messages/Mail or General share, then inspect the received file's duration, content, framing and audio. The untrimmed source must not be substituted.
5. Test portrait and landscape sources, MOV/MP4 imports, a silent video, full-range export and a long clip. Test portrait/landscape editor layout and repeated range changes. Report unsupported codec/HDR/color or export issues rather than treating build success as native media acceptance.
6. Cancel before saving and during a longer export, background during encoding, and switch tabs during saving. Confirm explicit outcome messages, no partial playable library entries and no damage to original/older memories. Check normal video playback and all three sharing entry points still work.

Shared tests validate exact range construction/validation/timestamp shapes and atomic, independent detail copying. Framework/host builds cannot verify actual encoded output, trim boundaries, audio/orientation, quality, native preview routing or physical-device cancellation. Those checks remain pending.

### Named video reels (core workflow device confirmed; layout refresh pending)

On October 20, 2026, the user confirmed the reel workflow worked on the physical iPhone. This records core workflow acceptance, not separate confirmation of every codec/audio/interruption case below.

The user selected videos only with original audio, a saved name, reorder controls, portrait 720x1280 layout and no title intro. This intentionally differs from Android's current silent export with a title intro; Android's existing implementation is unchanged.

- Open **Select > Create reel** (on the Memories tab), pick videos in the Memories grid, then tap **Next** to open **Edit Reel**. Photos and existing reels are excluded.
- Tap **Add Memories**, add at least two different videos, and use Back to return to the editor. The picker has metadata search and marks selected videos as Added. Enter a title and use the up/down arrows to set playback order. Remove only removes a draft selection, never a source file. Save stays disabled until a name and two videos are present. Draft selections are not persisted after leaving.
- Save joins the selected video tracks in that order, aspect-fitted into **720x1280 at 24 fps**, with black padding where needed. Native AVFoundation handles orientation and re-encoding. The first audio track of each source is retained at its original offset within that clip, limited to its video range; silent sources/track gaps remain silent. There is no added music, transition, title intro or burned-in metadata/zoom/overlays. Highest-quality MP4 is preferred, otherwise MOV; original codec, HDR/color and byte-identical quality are not promised.
- The new private video is saved before its name/ordered source keys are written atomically into shared local metadata. Success opens its existing player and adds a named **Reel** thumbnail under Reels. Thumbnail Share and header Select use the existing guardian/general sharing flow and send the completed file, not its sources. **All source memories are preserved.**
- If media saving succeeds but metadata fails, **Retry details** saves only metadata without encoding again; editing is locked so it cannot describe a different order. Leaving at that point retains the completed video as a normal unnamed video in Memories, with an explicit error explaining this. No unfinished reel record is published.
- Keep PlayerID foregrounded while building. Cancel export requests encoding cancellation; leaving the builder/tab requests cancellation too, and backgrounding cancels active encoding. A file already finalized is retained. Errors are visible and temporary-file cleanup failures are logged. There is no durable cross-launch export queue.
- Names and ordered source filename keys survive relaunch/container relocation. New reels inherit search/filter metadata from their sources, without assigning the output to an arbitrary team/player. A filter matches when **one source matches all filter facets**; all search words must occur in the reel name/type or aggregated source details/tags. An explicit Edit details assignment on the reel overrides inherited organization/search facets. Deleting or reorganizing source metadata can change inherited search/filter results, but never removes a finished reel's video or name.
- Trimming a reel copies its name/source references to the trimmed reel; these references record provenance, not exact remaining segments. Keeping/deleting the untrimmed reel follows the existing original-retention choice.

Run Command-R with the same bundle ID and installation; do not uninstall:

1. Add two or three recognizable videos, change their order, remove/add one, name the reel and Save. Verify saved order, approximate sum of playable source durations, no intro, and the saved name under Reels.
2. Mix portrait/landscape, camera/imported and silent/audio clips. Confirm upright aspect-fit video, black padding, 720x1280/24 fps output, correct audio at each join, sync, and silence only where expected. Check long clips and codec/HDR sources separately.
3. Verify every original remains unchanged. Relaunch and confirm reel playback/name, source order metadata, title/source-tag search, team/player filters and explicit reel Edit details behavior. Trim a disposable reel and test Keep/Delete without removing its source clips.
4. Share one reel and a selected batch through General share and guardian composers. Inspect received files for complete order/audio/framing, not source substitution. Confirm normal Memories import, playback, trimming and sharing still work.
5. Cancel before Save, during encoding, background during encoding and leave the tab while building. Check explicit outcomes, no partial playable files, no duplicate output on metadata-only retry, and unaffected older media/team data. After leaving a metadata-failed save, check Memories for the retained normal video.

Shared tests cover selection/reordering, durable names/source order, older-snapshot defaults, atomic metadata retry, source preservation, filtering/search and trim provenance. Android/framework/host builds do not verify encoded video/audio, output dimensions/frame rate, native cancellation or device UI behavior. Core reel workflow is device confirmed; individual edge cases and the layout refresh below remain pending. Scrub smoothness remains deferred and unresolved.

#### Android-aligned reel editor layout (device acceptance pending)

The next user-requested increment aligns reel creation/editing with Android's existing Edit Reel screen: surface-colored header with bold title and filled Save button, 158dp rounded preview area, bold Reel title field, "Memories in this reel" heading, 64dp rounded thumbnails, softly tinted 10dp-corner rows, stacked reorder arrows and a full-width Add Memories action. Adding sources now opens a separate Add Memories page, preserving the title/order and independent editor/picker scroll positions. Existing library filters still scope candidates; picker search narrows them further.

The preview plays **only the first selected memory**, explicitly labeled; it is not a rendered whole-reel preview. It reuses the existing native player and stops when entering Add Memories, changing the first memory, saving or leaving the screen. No second Compose controller is layered over the player. Unlike Android's 28dp reorder/remove controls, iPhone keeps 48dp touch targets for reliable tapping. Duration badges and fullscreen-preview controls are not fabricated. Export settings, original retention, source order and metadata-only retry remain unchanged.

Run Command-R without uninstalling. Check the header/title/rounded rows against Android, add/search sources and return without losing title/order, reorder/remove with long tags/opponents, and test keyboard/portrait/landscape scrolling. Play the first-memory preview, change the first clip, open Add Memories, save, switch tabs and background; old preview audio must stop. Verify saved output/sharing still work. Physical-iPhone visual/touch and preview lifecycle acceptance is pending.

### Android-aligned multi-screen layout pass (device acceptance pending)

The next user-requested pass continues layout/formatting alignment across all major shared screens while preserving confirmed iPhone workflows:

- **Memories/Reels library:** clearer header subtitle and stronger count emphasis while keeping existing tabs, search, filters, import/share flow and reel behavior.
- **Trim editor:** bottom controls now use a surfaced panel with consistent spacing and secondary copy styling; trim/export behavior is unchanged.
- **Post-recording review:** stronger title emphasis and consistent 64dp action chips/buttons for tag/share/delete/skip rows; no flow or persistence changes.
- **Team and roster pages:** Teams list now uses a clear top title, Team overview header reads "Team", and roster summary now shows visible/total player counts with a full-width Add player action.

No native adapters, persistence rules or export/share logic changed in this pass. It is visual/formatting only.

Run Command-R without uninstalling and verify: headers/spacing on Memories, Trim, Post-capture, Teams and Roster; long text wrapping; keyboard behavior; portrait/landscape; and that existing import/share/trim/reel/team actions still behave exactly as before.

#### Memories pixel-level alignment (device acceptance pending)

The latest focused pass compares the Memories collection chrome and thumbnail tiles directly with Android's `ClipsScreenRefactored`:

- Removed the extra descriptive subtitle to match Android's tighter search/filter stack. Kept the Memories/Reels title and top-header Select control for the accepted iPhone navigation/sharing interaction.
- The Photos import action is now an Add icon alongside the collection tabs, rather than a full-width text row. Its VoiceOver label is "Import from Photos"; import progress, success and per-item failure messages remain explicit.
- Search remains the Android-matched 48dp, 12dp rounded, 60%-surfaceVariant field. Filters remain horizontally scrollable. Collection tabs, divider, grid group heading and 8dp-spaced 124dp tiles retain the Android dimensions.
- Tile labels now use Android's tag-specific colors and labels (Score/Assist/Save/Defensive Stop/Faceoff Win/Ground Ball), or the saved reel name. Removed the duplicate lower-left tag label. Edit-details and direct Share controls remain on their prior corners; selection checkboxes and playback icons are unchanged.
- The import action has moved but not changed its picker/batch semantics. Reels remain reachable from their tab; reels are created from **Select > Create reel**.

Run Command-R without uninstalling. Compare the search/filter/tab/grid vertical rhythm and tile overlays with Android on the same device orientation. Check short/long tags, long reel names, selection checkboxes, play/share/edit hit targets, import VoiceOver name and import success/failure messages. Re-test search/filter/tab changes and sharing/import to ensure no action was lost. Physical-device visual and touch acceptance is pending.

#### Android-matched Memories cards, search and favorites (device acceptance pending)

- The Memories/Reels page title is gone. Select/Cancel now sits right of the + import button on both the Memories and Reels tabs, like Android. Successful or canceled shares no longer post a "Share completed"/"Sharing canceled" banner; share errors still appear.
- The search bar types out rotating ideas such as "Find all goals against <opponent>" using the first active player and a known opponent. Its Done button is replaced by the keyboard Search key and a mic button. Search ignores sentence filler words ("find", "all", "the", "against", "this season"…), possessive "'s" and plural "s", and matches "What happened" notes and "favorite".
- Voice search (`IosVoiceSearch`) uses Apple Speech recognition and the microphone, stops after a short pause or 12 seconds, and fills the search field. Tapping the mic again stops listening. Starting and stopping each give a medium haptic tap; haptics are allowed during recording so the stop tap is felt. While listening, a "Listening…" window with a pulsing mic is shown; it closes automatically after a pause in speech, and Cancel (or tapping outside) stops listening. `Info.plist` now includes `NSSpeechRecognitionUsageDescription`; denied permissions show an inline message pointing to Settings.
- The search field is now Ask Playloom with a search icon. Saying or typing a reel command, such as "Create a reel of all scores" or "Make a reel of Tyson's goals against Penn Trafford", uses shared `parseMemoryReelCommand` and structured metadata matching. Existing reels are excluded from source selection. Edit Reel opens for review; no export occurs until Save. Back cancels a result draft and returns to search. Typed commands run on Return. A match of 0 or 1 videos shows a message instead, because reels need at least 2. See Phase 4 for reviewed sharing.
- Spoken and typed searches use the same Android-matched matching (shared `searchCaptureLibrary`). Punctuation and hyphens are ignored, and moment phrases such as "face off wins", "face-off", "faceoffs", "goals" and "saves" match the canonical moment tag (Android's `canonicalMomentTag`). Dictation's spelling of "Face Off" therefore no longer causes empty results.
- Unassigned memories are grouped at the bottom of the library.
- Each card follows Android `RestoredClipGridTile`: a PlayerID share badge at the bottom-left (a white paper plane on a teal-to-coral gradient circle with a white ring, `MemoryShareBadge`), no play icon over videos, a favorite star at the bottom-centre (`captureFavorites`), the ⋯ Memory actions menu at the bottom-right, and a "What Happened" row under every thumbnail (reels included since October 22, 2026) that opens an Add/Save note editor (`captureNotes`, 280 characters). The ⋯ menu shows Edit memory and Remove memory. Remove confirms first, then deletes the file from PlayerID's private media folder and forgets its details.
- The Share screen preview no longer shows a "Team memory" placeholder when a memory has no date.
- Follow-up: group headings no longer show the team name or "vs". They show the opponent with an Android-style "MMM d, yyyy" date in a lighter font underneath. The date comes from the linked game, or from the capture time in the PlayerID filename. Groups without an opponent show no date. Each opponent and game date gets its own group, so PR on Oct 23 and PR on Nov 12 appear as two separate sections.
- Memories/Reels tabs, + and Select stay on one line; screens narrower than 400 points use tighter padding and smaller labels.
- Edit memory now opens the full-screen Android-style "Trim memory" editor for videos: back arrow and title, black preview, handle trim bar, start/selected/end times, and Full memory / Save buttons. Save keeps the accepted keep-or-delete-original choice. Photos still open Edit details.
- Remove memory reads "1 memory will be removed from the app. The original will remain on your phone." It deletes PlayerID's own copy; imported items keep their Apple Photos original.
- The What Happened editor hides the keyboard when you press Done, tap outside the field, or tap Add/Save/Cancel.
- **Reels tab (October 22, 2026):** At the user's request, the Reels tab no longer shows the team/season/opponent breadcrumb filters, and those filters don't apply to reels; search still does. Reels are grouped by the year they were created (taken from the timestamp in the reel's filename, `groupReelsByYear`), newest year first. Reels with no timestamp go under **Undated**, last. Reel cards now have the same **What Happened** row as memories. The Memories tab is unchanged. Test: `MemoryReelRepositoryTest.reelsAreGroupedByCreationYearNewestFirstIgnoringTeamAndOpponent`.
- **Search by year:** The action bar matches the year a memory was captured or a reel was created, so "Show me all the reels for 2026" lists 2026 reels. Search never switches tabs: the Reels tab searches only reels and the Memories tab only memories. Test: `MemoryReelRepositoryTest.searchingAYearFindsReelsCreatedThatYear`.
- **Reel cards:** Reel cards no longer show the reel-name tag. Their ••• menu shows **Edit Reel** (opens Edit Reel) and **Remove Reel**.
- **Reels Select menu and Year dropdown:** On the Reels tab, Select offers **Remove reels** and **Share reels** (Create reel is only on the Memories tab). The selection hint and remove dialog say "reels". A **Year** dropdown sits where the breadcrumbs were. It lists only years that have reels, plus **All years**, and filters the grid; Clear search and filters resets it. The "Import canceled." message was removed; a canceled classification that kept items shows only "N item(s) were kept as Unassigned."

Check that the favorite star persists after relaunch, that the note row persists and is searchable, and that Remove memory deletes immediately. Also check that voice search permissions, listening and stop work, and that video playback audio still works after using voice search.

### Camera fit, light appearance and Game setup (device acceptance pending)

The follow-up sizing pass reduces the iPhone shutter ring from 64dp to 48dp (with proportionally smaller inner photo/record/stop visuals), mode-button minimum height from 80dp to 56dp, and top-left setup pill padding, jersey/chevron sizes and typography. Android retains its original component defaults. Recheck readability, mode selection, setup opening and photo/record/stop touch handling on the phone; these size changes still require device acceptance.

The subsequent spacing adjustment moves the collapsed roster handle from a 196dp to 168dp bottom offset, leaving an 8dp gap above the standard-height mode row, and reduces the compact team/opponent separator dot from 7dp to 4dp. Expanded drawer behavior and Android's separator size are unchanged. Check handle tapping/swiping and pill readability on iPhone.

The iPhone shell's bottom menu is now 56dp tall instead of 64dp, with a 32dp selected-icon highlight instead of 40dp. This applies consistently across iPhone tabs; Android retains its existing 64dp default. Check tab touch targets, icon centering and home-indicator clearance on the physical phone.

The latest camera refinement tightens the compact setup pill's team/player line heights to 15sp/12sp and removes the inter-row gap, retaining the existing text sizes. The iPhone shutter and its touch area increase from 48dp to 56dp with proportionally scaled inner visuals; Android defaults remain unchanged. Check pill readability with larger text and photo/record/stop interaction on the physical phone.

Compact Photo/Rolling/Video buttons now use slightly darker black-tinted backgrounds; selected modes keep their bold label and selection dot. Android's default button styling is unchanged. Game setup now applies keyboard insets to the sheet's outer viewport before calculating its maximum height, keeping the scrollable sheet immediately above the keyboard instead of leaving a camera gap. The inline Done button inside Opponent is removed; the keyboard's Done action still dismisses typing, and the sheet's bottom Done action still saves. Check keyboard opening/dismissal, scrolling to Cancel/Done, saving and reopening the opponent on the phone.

The user reported that the keyboard gap persisted after the inset adjustment. The next correction removes the native preview view while Game setup is open, replacing it with a black backdrop so native camera rendering cannot cover the keyboard-panned Compose form. The camera session remains active and the preview is recreated on dismissal. This correction still requires physical-device confirmation; the prior inset-only change did not resolve the reported behavior. The sport/team selector and Opponent now share the same team-accent border colors, and sheet padding, section gaps and expanded-detail gaps are tighter without reducing field/button touch targets. Check the complete form above the keyboard, Cancel/Done, preview restoration, pinch zoom and capture after dismissing setup.

The user reported overflowing camera modes and different bottom-menu/Game setup colors, then chose a layout-only pass with a light menu and light Game setup. Rolling implementation is explicitly deferred.

- Photo, Rolling and Video now share the available camera width equally with 10dp side margins and 6dp gaps, instead of scrolling three minimum-136dp buttons. iPhone uses compact 18sp labels and 8dp button padding; Android retains its existing default control sizes. Rolling remains disabled and now explicitly says **Unavailable**. Photo/video capture and the shutter behavior are unchanged.
- The iPhone Camera destination, including Game setup and its bottom navigation, uses the existing shared light brand palette even when the phone is in Dark Mode. Other destinations still follow system appearance. Android's theme and navigation call sites are unchanged.
- Game setup matches Android's compact primary structure: title, YOUR TEAM, sport-grouped team menu, jersey swatches, divider and MATCHUP/opponent field. Team accent colors now drive section headings, selected jersey outline and opponent field styling. The redundant Team field label is removed.
- iPhone no longer shows the player/season/scheduled-game fields (the **Player and schedule** section was removed); jersey colors stay under YOUR TEAM with an info icon. Current-game suggestions remain visible, and Cancel/Done retain their existing draft/save semantics. The sheet remains scrollable and keyboard-aware on smaller screens.

Run Command-R without uninstalling. Only the Camera tab and the full-screen memory viewer rotate (closing the viewer returns Memories to portrait); in the landscape viewer the play button and progress slider sit together in one row along the bottom edge, inset from the notch: the Memories grid and the Settings tab (More, Teams, Settings) and onboarding stay portrait. Shared `WorkspaceRoot` reports `onRotationAllowed`; iOS stores it in `IosOrientationLock`, which the Swift `AppDelegate` (`supportedInterfaceOrientationsFor`) reads, and it rotates the window back to portrait when you leave Camera. The bottom tab bar is hidden whenever the screen is landscape; rotate to portrait to switch tabs. In landscape the Photo/Rolling/Video buttons stack vertically on the right edge (centred above the roster drawer when it is open); in portrait they stay in a row above the shutter. Confirm all three mode labels fit at once in portrait/landscape, including the smallest supported screen and increased text size. Verify Photo/Video switching, recording/stop, roster drawer positioning and zoom; Rolling must remain disabled with its explanatory label. Check the light bottom menu and Game setup in both system Light and Dark Mode, team accents, grouped team selection, jersey colors, opponent keyboard visibility, and the jersey color info message. Cancel must preserve prior setup; Done must save it and apply only to new captures. Device layout/touch acceptance remains pending; build success does not establish visual parity.

### iPhone Rolling capture (device acceptance pending)

The user subsequently approved enabling Rolling with Android's 30-second workflow. This supersedes the earlier disabled/unavailable status above. Selecting Rolling starts temporary video/audio recording immediately; tapping the animated shutter exports the latest 30 seconds of available footage (less during initial warmup), saves the current camera organization, and opens the existing clip review/share screen. Buffering resumes after dismissing review. Switching modes, leaving Camera or backgrounding discards unsaved footage, rather than creating an unsolicited memory. Recording and Rolling buffering both prevent foreground idle auto-lock.

The native adapter rotates temporary MOV recordings approximately every 30 seconds, retaining one completed segment plus the active recording; export selects the trailing 30 seconds across those segments without re-encoding. Rotation briefly finalizes/restarts native recording, so a small real-time video/audio gap at boundaries is possible; this is not yet verified as seamless capture on hardware. Orientation is fixed when the buffer starts. The shared controller handles mode/state/lifecycle/error behavior and the shared window calculation limits export duration; AVFoundation recording and composition stay in the iPhone adapter. Temporary segments never enter Memories. Cleanup failures are reported, or logged if the requested saved clip has already committed; stale Rolling-only files are cleaned when starting a stopped camera session.

The compact game-info pill now has a 3dp gap between its team and player rows, restoring a little spacing while retaining the smaller line heights and 56dp shutter.

Run Command-R without uninstalling and check:

1. Select Rolling and save after roughly 5 seconds. Confirm a short video with audio, current team/player/opponent, review and sharing. Close review and confirm buffering restarts.
2. Wait over 40 seconds, then save. Confirm an approximately 30-second clip containing action from before the tap, not an entire-session recording. Repeat near 30/60-second boundaries and check audio, timestamps and any boundary discontinuity. Repeat for several minutes to exercise buffer pruning.
3. Switch to Photo/Video without saving, or leave Camera/background while buffering. Confirm no unwanted memory appears; return and confirm a fresh buffer. Check interruptions and denied permissions/storage failures for visible errors.
4. Recheck ordinary Photo/Video capture, record/stop, pinch zoom after setup dismissal, roster selection, clip review deletion and existing Memories. Check the pill spacing and Rolling label at increased text size.

Builds and shared state/window tests do not establish native audio continuity, encoded duration or device touch/lifecycle acceptance.

#### Rolling polish and Android-matched clip review (device acceptance pending)

Creating a reel now follows the Android flow. Choose **Select → Create reel**, and the Memories grid becomes the picker, showing only videos with a "Select memories · N selected · Total m:ss" bar. Tap **Next** to open **Edit Reel**, which shows a preview with the total time and a full-screen button, the reel title (default "<Team> Highlight Reel"), and the memories in order with up/down/remove controls. **Add Memories** and **Back** return to the grid with the selection, order and title kept. Saving shows **Reel ready** with **View** and **Share**. Reels are never trimmed. **Edit memory** on a reel (or the edit button in the reel viewer) opens **Edit Reel** with its saved memories and title. Back closes the editor, and Save replaces the old reel while keeping its favorite and note. Every newly built or edited reel opens with Android's 1.6 s intro card, drawn by `IosMemoryReelExporter`. The card has a #0A1118 background, #1C8CFF bars at the top and bottom, "HIGHLIGHT REEL", the title wrapped by the shared `reelIntroTitleLines` (18 characters per line, 3 lines max), and "Created with PlayerID". The clips and their audio start after the card. Reels saved before this change don't get the card until they are edited and saved again. As before, the iPhone exporter still needs at least two videos (Android allows one).

Memories **Select** now opens the Android menu: **Remove memories** (pick memories, then confirm "# memories will be removed from the app…"), **Create reel** (opens the reel picker), and **Share memories** (pick memories, then Share). In Reels, Select still goes straight to sharing. The camera no longer flashes "Camera paused / Resume camera" while it starts. That card now only appears for real failures.

Game setup no longer removes the live camera preview while the sheet is open. The preview is hidden instead. Re-creating it re-attached a preview layer to the running capture session, which delayed closing the sheet and ended the Rolling recording ("Rolling recording stopped unexpectedly"). Cancel and Done should now close instantly, and Rolling should keep buffering.

The user reported that haptics and immediate dismissal were still not working after the previous pass. Deletion now dismisses review synchronously on the tap rather than awaiting file/metadata operations. Cleanup runs in the owning camera scope, not the dismissed review's scope; progress and explicit retryable errors appear on the camera. The saved reference/count is cleared only after physical file deletion succeeds. The audio adapter now owns PlayAndRecord/VideoRecording audio-session configuration instead of allowing AVCaptureSession to replace it automatically, and reapplies the recording-haptics allowance after movie startup and before each impact. The shutter now requests a heavy impact. Verify actual haptic sensation, video/audio recording, external audio routing, immediate review disappearance and deletion/retry behavior on the phone; compilation cannot establish these behaviors.

The user confirmed Command-R deployment but still reported delayed dismissal and no haptic sensation. The next pass moves native movie start/stop calls off the UI thread so resuming Rolling cannot synchronously block the review-to-camera transition. Shutter impact is now requested on touch-down through its interaction source, rather than after release in the capture callback; no audio-session configuration runs in the touch handler. Xcode console diagnostics record receipt of a shutter press with the audio session's haptic allowance, and synchronous review dismissal before asynchronous deletion. These changes require another device check and are not yet confirmed fixes.

On the physical iPhone, the user confirmed shutter haptics now work (impact generator plus system sound 1519, with recording haptics allowed by the audio session). Console logs showed review dismissal ran immediately, but the visual return to Camera was still delayed. The review previously replaced the camera page, so dismissal recreated the native preview view and reattached its preview layer to the running session. Review is now a touch-blocking overlay above a camera page that remains mounted, keeping the preview alive. Preview pinch zoom is disabled while review is visible. Instant Delete dismissal awaits device confirmation.

The iPhone camera now starts in Rolling by default (Android's existing default is unchanged). All iPhone post-capture Delete actions now bypass confirmation: successful private-file and metadata deletion dismisses review and clears its saved camera reference. Storage errors remain visible for retry rather than claiming deletion succeeded. Record-button taps now produce impact feedback in every mode, and the native audio session explicitly permits haptics during microphone recording; otherwise iOS can suppress the pulse during Rolling/Video. Check cold-start buffering, immediate Video/Rolling deletion and dismissal, absence from Memories, and haptics while actively recording. Deletion removes PlayerID's actual file from phone storage, not a separate untracked copy manually exported to Photos.

After **Share Memory** completes on the post-capture review, the review stays open with "Shared. Tag what happened below, or tap Skip." so the new clip can still be tagged. Tapping a tag or Skip closes it as before.
The quick actions are now a compact button row (filled **Share**, red outlined **Delete**, text **Skip**) above a divider; each button sizes to its label so no word is truncated, so they no longer look like the What happened tag cards.
In landscape, the post-capture review places all predefined tags and **Custom** in one horizontal row, with reduced panel padding/spacing so the quick actions and predefined tags are visible together. Portrait keeps the two-column tag layout. Saved custom tags remain below and can be reached by scrolling; tagging, sharing, deleting and skipping behave as before. Check on a physical iPhone in both landscape directions, including returning from Share and rotating while the review is open.

Rolling review now deletes immediately without the app's "Delete this clip?" confirmation. It removes the private saved video and its organization/tag details, clears the camera's saved-clip reference/count, then returns to fresh buffering. Ordinary Video review retains confirmation. Errors remain visible and metadata-cleanup failures can be retried. PlayerID does not automatically create a Photos/Camera Roll copy of recordings, so no Photos asset exists to remove at this stage; a copy separately saved through external sharing is not tracked or deleted. Check immediate Rolling deletion, absence from Memories, buffer resumption, and ordinary Video confirmation on the physical phone.

The Rolling explanation now appears for five seconds on first successful use only. An iPhone preferences flag remembers that it was shown across app restarts; the persistent explanation is removed. Segment rotation now leaves the shared UI in Buffering instead of switching through Starting, eliminating the scheduled loading/reset state and idle-lock/zoom toggles every 30 seconds. Shutter taps arriving during rotation wait for it to finish, then save, rather than silently being dropped. This does not change native stop/restart segmentation or establish seamless video/audio across segment boundaries.

Video start/stop and Rolling save taps now trigger a native medium-impact haptic, corresponding to Android's single record-button pulse. Photo taps remain unchanged, matching Android. The exact iPhone feel depends on hardware/system haptic settings.

The "What happened?" review now follows Android's active camera prompt: 96%-width bottom sheet, 18dp row gaps, 80dp rounded surface-variant cards with 2dp elevation, the same lacrosse/glove/flag/target/shield moment symbols, and 22dp action icons. Custom tags use label cards with an Edit/Done toggle and rename/remove controls; their touch targets remain at least 48dp rather than copying Android's smaller edit icons. Delete retains its error-container colors and confirmation; Share Clip, Skip, custom tagging and explicit save/share/delete errors retain their existing behavior. Removed the extra introductory/share explanation paragraphs, but operation-specific errors and cancellation feedback remain visible.

Run Command-R on the existing installation. Confirm the hint disappears after five seconds and never returns on later selections/restarts. Stay in Rolling beyond 30 and 60 seconds: there should be no UI loading reset; tap Save near a rotation and verify one saved clip. Feel haptics on Video start/stop and Rolling save. Compare the review's icons, colors and layout with Android, and exercise every tag, custom add/rename/remove, Share/cancel, Delete/cancel and Skip. Check scrolling on smaller screens/large text. Native segment continuity and device appearance/haptics remain pending phone acceptance.
### Teams parity with Android

- The Teams list, overview, roster, and schedule use the same shared Compose pages as Android (`TeamSelectionPage`, `TeamOverviewPage`, `RosterPage`, `SchedulePage`).
- iPhone now matches Android's host wiring: no extra "← More" button (use the More tab), the standard "Join Team" label and dialog title, team `color` for cards and the overview header, and the team description (or sport) as the overview subtitle.
- Roster rows show Android's "N memories" badge beside the favorite star. Counts come from the shared `playerMemoryCounts`, which uses memories tagged to each player and excludes reels.
- Still Android-only: player photo upload on roster avatars, team invites (Overview hides Invite on iPhone), and the post-create team setup workflow.
- The Memories header's Select/Cancel uses the same title style as the Memories/Reels tabs.

## Roster import parity with Android

Teams → team → **Import roster** opens the same shared options dialog as Android ("From screenshot", "From app", "From website"). Shared Kotlin owns the review UI, parsing, and import (`ui/team/RosterImportScreens.kt`, `roster/RosterImportSources.kt`); `IosRosterImportAdapter` only provides the Photos picker, Vision text recognition, and a `WKWebView`.

- **From screenshot:** pick one image → "Import from Screenshot" review (remove players, duplicate jersey numbers block import) → "Overwrite Existing Roster?" → "Import Complete" auto-closes and opens the Roster page.
- **From app:** Android records the roster app's screen, which iOS does not allow. iPhone shows instructions to screenshot the roster in TeamSnap/GameChanger/etc. and then reads several screenshots at once.
- **From website:** address bar (URL or Google search), back/forward/refresh, **Capture** reads HTML roster tables with Android's script (with the "Multiple rosters found" picker) and falls back to scrolling the page and reading it with text recognition.
- `Info.plist` sets `NSAllowsArbitraryLoadsInWebContent` so older `http://` school roster sites load in the web view only.

## Schedule import parity with Android

Teams → team → **Import schedule** opens Android's "Import schedule" options dialog, then the shared "Import Schedule" screen (`ui/team/ScheduleImportPage.kt`). Games are parsed by the shared `parseScheduleText`, shown as Android's checkbox list ("Selected X of Y games", Select all / Clear all), and saved with "Import N Games"; the app then opens the Schedule page. Exact repeat games are skipped.

- **From screenshot:** "Choose Schedule Screenshot" reads one image with Vision text recognition.
- **From app:** Android captures the sports app's screen; iPhone asks for schedule screenshots from that app and reads several at once.
- **From website:** "Schedule website" field (address or search) with a built-in browser starting at Google, and "Capture Schedule" running Android's page script.
- Games whose end time is not after the start time are shown with a warning and cannot be selected.

### Renaming a team

In a team, open Team settings (subtitle "Team name, sport and jersey colors"). The Team Name field can be edited, and similar-name warnings ignore the team's own name. Saving with a new name updates, in one save, every record linked to the team: roster players, subscriptions, seasons (and through them, games), the assigned player, memory/capture organization, the camera's current team and saved share contacts. Names that already exist (ignoring case) are rejected. The team stays open after it is renamed. Android does not yet have a team rename.

### Memories opponent filter and suggestions

The Memories Opponent filter now lists only opponents that have at least one saved memory matching the chosen team, season and player (memories whose files are gone are ignored). Scheduled games no longer add opponents to this list. In "Edit memory details", typing in the Opponent field shows up to five matching opponents from that team's schedule (only the chosen season, if one is set). Opponents whose names start with the typed text come first; tap one to fill the field.

Each opponent is stored with one spelling (`OpponentNames.kt`, applied by `LocalTeamRepository` on load and on every save). Names that differ only in capitalization, spaces or punctuation ("Pine Richland", "PINE-RICHLAND") are merged. The schedule's spelling wins, and all-caps names are shown in normal capitalization ("Pine-Richland"); short one-word acronyms such as "CAN" stay as typed. An abbreviation such as "PR" or "USC" becomes the full name only when exactly one known opponent of that team has those initials. Typing initials in the Opponent field also suggests the matching full name. Existing memories are merged in memory at launch and written to disk with the next change.

Memories group headers show the date each clip was captured (the time stored at the start of its file name). The linked game's date is used only when a file has no stored time. Photos imports now keep the original recording time: videos use their QuickTime creation date and photos their EXIF `DateTimeOriginal`. When that metadata is missing, the import time is used. Clips imported before this change keep their import time, so re-import them to correct their dates.

The Memories Player filter lists only each team's "My Player" (chosen on the team page in Teams), narrowed to the selected team when one is set. A player filter for someone who is no longer My Player is cleared automatically. On the team page, the Import roster and Import schedule links now sit directly under their Roster and Schedule rows.

The rotating Memories action-bar examples name only My Player(s) (first name), e.g. "Make a reel of Tyson's saves". With no My Player set, player examples are left out instead of naming a random roster player.

On the Teams list, Join Team and Create Team pills size to their labels (each up to half the row) and never wrap or truncate; if a label would not fit, its font shrinks (down to 10sp). Untagged memories show a "Tag" badge instead of "Memory".

### Edit memory (trim slider + details on one screen)

For a video, **Edit memory** (from the thumbnail's ⋯ menu; the full-screen viewer has no Edit memory button) opens one screen. The video preview is at the top, with a Photos-style trimmer under it: a filmstrip of frames framed by chevron handles. Drag either handle to trim. The frame turns yellow once the clip is trimmed, frames outside the selection are dimmed, and the label above shows the selected length, or the handle's time while you drag. On iPhone the frames are generated with AVAssetImageGenerator; without frames the strip shows a plain track. Below the slider are Android's **Edit tag** and **Edit opponent** editors, followed by team, player, season and game. Photos and reels keep the **Edit memory details** dialog, which has the same fields.

- **Tag:** a dropdown menu listing **No tag**, the default tags and your custom tags, with a checkmark on the current one. **Custom tag...** opens a field; **Add tag** (or Done on the keyboard) adds and selects it, and it is remembered when you tap Save.
- **Player** choices list only the team's **My Player(s)** from Teams. A player already saved on the memory is kept unless you change it.
- **Opponent:** an "Enter opponent" field. Suggestions appear only after you start typing: up to five opponents from the team's schedule and from opponents already saved on its memories. Names that start with what you typed come first. There is no separate Game picker; typing an opponent replaces any scheduled-game link. Tap one to fill the field; clear the text to remove the opponent. A typed name that matches an existing opponent (ignoring case) is saved with that opponent's spelling.
- **Save:** if the slider still covers the whole video, Save only updates the details. If you trimmed it, Save updates the details and adds a trimmed copy with those details. The original is always kept, and no prompt is shown.
- Nothing is written until **Save**; Back or **Cancel** discards all changes.
- **Season** shows just the year. Tapping it opens an inline iOS-style wheel showing three rows (None, then next year back to 1990, plus the saved year if outside that range); the row that snaps into the highlighted centre band is selected, and tapping a row scrolls to it. Saving a year the team has no season for creates an inactive "2024 Season" for that team (or reuses a season with that year), so it never replaces the current season used by schedule import and Camera.
- Tag, Team, Player, Season and Opponent are separate form rows: a fixed-width label on the left and a right-aligned, single-line value with a chevron (long values end in "…" instead of wrapping). Season shows "Import a schedule" when the team has none.
- The trim bar scrolls with the details below it, so scrolling down hides it while the preview stays in place.
- The preview's playback scrubber is hidden on this screen; the filmstrip trimmer is the only bar. The play button remains. **Tag** and **Opponent** are always visible; Opponent appears once a team is chosen.


- iPhone uses `ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing })` so focusing fields (such as Opponent in Game setup) does not force-lift the entire screen; the sheet still uses keyboard insets for visibility. Because automatic lifting is off, screens with text fields manage the keyboard themselves: Edit memory uses `imePadding` so it ends at the keyboard, hides the video preview while the keyboard is open, and scrolls the Opponent field and its suggestions into view when focused or while typing. The app shell consumes the tab bar's height from the keyboard inset (`consumeWindowInsets`), so sheets above the tab bar rise only to the top of the keyboard with no gray gap.
