const fs = require('fs');
const path = require('path');

// - Pen.deV schema helpers - - - - - - - - - - - - -
// Produces VALID pen.deV v2.19 nodes:
// * lowercase node types (frame / rectangle / ellipse / text / path)
// * `fill` (not `background`), `cornerRadius`, `stroke`/`strokeWidth`
// * text uses `content` + direct text props (fontSize/fontWeight/fill/...)
// * shadows go through `effect: {type:"shadow", shadowType:"outer", ...}`
// * colors normalized to #RRGGBBAA (rgba() is not accepted)
// - - - - - - - - - - - - - - - - - - -

function normalizeColor(c) {
 if (c == null) return c;
 if (c === 'transparent') return '#00000000';
  const m = /^rgba\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*([0-9.]+)\s*\)$/i.exec(c.trim());
 if (m) {
 const r = (+m[1]).toString(16).padStart(2, '0');
 const g = (+m[2]).toString(16).padStart(2, '0');
 const b = (+m[3]).toString(16).padStart(2, '0');
 const a = Math.round((+m[4]) * 255).toString(16).padStart(2, '0');
 return ('#' + r + g + b + a).toUpperCase();
 }
  if (/^#[0-9a-fA-F]{3,8}$/.test(c.trim())) return c.trim().toUpperCase();
 return c;
}

function shadowEffect(x = 0, y = 6, blur = 18, color = '#00000026') {
 return {
 type: 'shadow',
 shadowType: 'outer',
 color: normalizeColor(color),
 offset: { x, y },
 blur: blur
 };
}

function frame(name, x, y, width, height, fill = '#FFFFFF', cornerRadius = 0, children = [], effect = null) {
 const obj = {
 type: 'frame',
 name: name,
 x: x,
 y: y,
 width: width,
 height: height,
 fill: normalizeColor(fill),
 layout: 'none',
 clip: true
 };
 if (cornerRadius) obj.cornerRadius = cornerRadius;
 if (effect) obj.effect = effect;
 if (children && children.length > 0) obj.children = children;
 return obj;
}

function rect(name, x, y, width, height, fill = '#0A0A0A', cornerRadius = 0, effect = null, extra = {}) {
 const e = { ...extra };
 if ('fill' in e) e.fill = normalizeColor(e.fill);
 if ('stroke' in e) e.stroke = normalizeColor(e.stroke);
 if ('shadow' in e) {
 const s = e.shadow; delete e.shadow;
 e.effect = { type: 'shadow', shadowType: 'outer', color: normalizeColor(s.color || '#00000026'), offset: { x: s.x || 0, y: s.y || 0 }, blur: s.blur || 16 };
 }
 const obj = {
 type: 'rectangle',
 name: name,
 x: x,
 y: y,
 width: width,
 height: height,
 fill: normalizeColor(fill),
 ...e
 };
 if (cornerRadius) obj.cornerRadius = cornerRadius;
 if (effect) obj.effect = effect;
 return obj;
}

function ellipse(name, x, y, width, height, fill = '#F5F5F5', effect = null, extra = {}) {
 const e = { ...extra };
 if ('fill' in e) e.fill = normalizeColor(e.fill);
 if ('stroke' in e) e.stroke = normalizeColor(e.stroke);
 if ('shadow' in e) {
 const s = e.shadow; delete e.shadow;
 e.effect = { type: 'shadow', shadowType: 'outer', color: normalizeColor(s.color || '#00000026'), offset: { x: s.x || 0, y: s.y || 0 }, blur: s.blur || 16 };
 }
 const obj = {
 type: 'ellipse',
 name: name,
 x: x,
 y: y,
 width: width,
 height: height,
 fill: normalizeColor(fill),
 ...e
 };
 if (effect) obj.effect = effect;
 return obj;
}

function text(name, x, y, content, fontSize = 14, fontWeight = '400', fill = '#0A0A0A', extra = {}) {
 const weight = String(fontWeight) === 'normal' ? '400' : String(fontWeight);
 const e = { ...extra };
 if ('fill' in e) e.fill = normalizeColor(e.fill);
 return {
 type: 'text',
 name: name,
 x: x,
 y: y,
 content: content,
 fontSize: fontSize,
 fontWeight: weight,
 fontFamily: 'Inter',
 fill: normalizeColor(fill),
 ...e
 };
}

function pathNode(name, x, y, width, height, geometry, viewBox, fill = '#0A0A0A', extra = {}) {
 return {
 type: 'path',
 name: name,
 x: x,
 y: y,
 width: width,
 height: height,
 geometry: geometry,
 viewBox: viewBox,
 fill: normalizeColor(fill),
 ...extra
 };
}

function icon(name, x, y, size, glyph, fill = '#0A0A0A', library = 'Material Symbols Rounded') {
 return {
 type: 'icon',
 name: name,
 x: x,
 y: y,
 width: size,
 height: size,
 library: library,
 icon: glyph,
 fill: normalizeColor(fill)
 };
}

// - - - - - - - - - - - - - - - -
// Screen 00: Splash & Onboarding
// - - - - - - - - - - - - - - - -
function createScreenSplash(x, y) {
 const children = [
 text('Skip', 310, 56, 'Skip', 14, '500', '#888888'),
 
 // 3D Illustration Avatar Stack
 ellipse('Avatar Shadow Base', 70, 130, 235, 235, '#E5E5E5', shadowEffect(0, 16, 24, '#0000001f')),
 ellipse('Avatar Card', 62, 120, 251, 251, '#F8F8F8', shadowEffect(0, 8, 20, '#00000015'), {
 stroke: '#0A0A0A',
 strokeWidth: 2
 }),
 icon('Icon Anchor', 140, 215, 72, 'anchor', '#0A0A0A'),

 // Headline & Subtitle
 text('Title Line 1', 32, 410, 'Stay anchored', 32, '700', '#0A0A0A'),
 text('Title Line 2', 32, 450, 'to what matters.', 32, '700', '#0A0A0A'),
 text('Subtitle', 32, 510, 'Anchor menyadari ketika kamu menjauh dari tugas\nyang penting - dan membantu kamu kembali,\ntanpa menghakimi.', 14, '400', '#666666', { lineHeight: 1.5 }),

 // 3D Raised CTA Button
 rect('CTA 3D Depth Base', 32, 718, 311, 56, '#222222', 16),
 rect('CTA Button Front', 32, 712, 311, 56, '#0A0A0A', 16, shadowEffect(0, 10, 24, '#00000040')),
 text('CTA Text', 124, 730, 'Get started', 16, '600', '#FFFFFF')
 ];

 return frame('00 - Splash & Onboarding', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 00b: Permission Onboarding
// - - - - - - - - - - - - - - - -
function createScreenPermissions(x, y) {
 const children = [
 text('Step Indicator', 32, 60, 'Setup 1 of 2', 13, '500', '#888888'),
 rect('Progress Track', 32, 84, 311, 4, '#EEEEEE', 2),
 rect('Progress Bar', 32, 84, 155, 4, '#0A0A0A', 2),

 text('Headline 1', 32, 116, 'Smart Todo works', 28, '700', '#0A0A0A'),
 text('Headline 2', 32, 150, 'right away.', 28, '700', '#0A0A0A'),
 text('Sub', 32, 192, 'Izin ini opsional - buka fitur lengkap Anchor\nkapan pun kamu siap.', 14, '400', '#666666', { lineHeight: 1.4 }),

 // 3D Card 1: Usage Access
 rect('Perm 1 Shadow Base', 32, 260, 311, 80, '#E5E5E5', 18),
 rect('Perm 1 Card Front', 32, 254, 311, 80, '#F9F9F9', 18, shadowEffect(0, 6, 16, '#0000000f')),
 icon('Perm 1 Icon', 52, 276, 28, 'bar_chart', '#0A0A0A'),
 text('Perm 1 Title', 98, 272, 'Usage Access', 15, '600', '#0A0A0A'),
 text('Perm 1 Desc', 98, 296, 'Untuk Wellbeing & deteksi drift', 13, '400', '#777777'),
 text('Perm 1 Tag', 268, 282, 'Opsional', 11, '500', '#999999'),

 // 3D Card 2: Notifications
 rect('Perm 2 Shadow Base', 32, 356, 311, 80, '#E5E5E5', 18),
 rect('Perm 2 Card Front', 32, 350, 311, 80, '#F9F9F9', 18, shadowEffect(0, 6, 16, '#0000000f')),
 icon('Perm 2 Icon', 52, 372, 28, 'notifications', '#0A0A0A'),
 text('Perm 2 Title', 98, 368, 'Notifications', 15, '600', '#0A0A0A'),
 text('Perm 2 Desc', 98, 392, 'Intervensi lembut & sesi fokus', 13, '400', '#777777'),
 text('Perm 2 Tag', 268, 378, 'Opsional', 11, '500', '#999999'),

 // Privacy highlight card
 rect('Privacy Shadow Base', 32, 452, 311, 56, '#222222', 14),
 rect('Privacy Card Front', 32, 448, 311, 56, '#0A0A0A', 14, shadowEffect(0, 8, 20, '#00000030')),
 icon('Privacy Icon', 52, 466, 16, 'lock', '#FFFFFF'), text('Privacy Text', 74, 466, 'Semua data tersimpan lokal di HP.', 13, '500', '#FFFFFF'),

 // 3D Action buttons
 rect('Continue Base', 32, 718, 311, 56, '#222222', 16),
 rect('Continue Front', 32, 712, 311, 56, '#0A0A0A', 16, shadowEffect(0, 10, 24, '#00000035')),
 text('Continue Text', 128, 730, 'Lanjutkan', 16, '600', '#FFFFFF'),
 text('Skip Link', 144, 780, 'Lewati dulu', 14, '400', '#888888')
 ];

 return frame('00b - Permission Onboarding', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 01: Home - Smart Todo (Today)
// - - - - - - - - - - - - - - - -
function createScreenHome(x, y) {
 const children = [
 text('Greeting', 32, 60, 'Good morning', 13, '500', '#888888'),
 text('Date Title', 32, 80, 'Wednesday, Oct 7', 24, '700', '#0A0A0A'),

 // 3D Hero Progress Card
 rect('Prog Shadow Base', 32, 130, 311, 88, '#262626', 20),
 rect('Prog Card Front', 32, 124, 311, 88, '#0A0A0A', 20, shadowEffect(0, 12, 30, '#00000045')),
 text('Prog Subtitle', 52, 144, 'Today\'s progress', 12, '500', '#AAAAAA'),
 text('Prog Count', 52, 164, '1 of 3 tasks completed', 18, '700', '#FFFFFF'),
 rect('Prog Track', 52, 194, 180, 4, '#333333', 2),
 rect('Prog Fill', 52, 194, 60, 4, '#FFFFFF', 2),
 text('Prog Pct', 264, 156, '33%', 28, '700', '#FFFFFF'),

 // Tabs
 text('Tab All', 32, 236, 'All', 14, '500', '#888888'),
 text('Tab Today', 70, 236, 'Today', 14, '700', '#0A0A0A'),
 rect('Tab Indicator', 70, 256, 40, 2, '#0A0A0A', 1),
 text('Tab Upcoming', 126, 236, 'Upcoming', 14, '500', '#888888'),
 text('Tab Completed', 214, 236, 'Completed', 14, '500', '#888888'),

 // 3D Task Card 1 (Active High Priority)
 rect('Task 1 Shadow Base', 32, 280, 311, 88, '#D8D8D8', 16),
 rect('Task 1 Front', 32, 274, 311, 88, '#FFFFFF', 16, shadowEffect(0, 8, 20, '#00000010'), {
 stroke: '#EAEAEA', strokeWidth: 1
 }),
 rect('Task 1 Priority Bar', 32, 274, 4, 88, '#0A0A0A', 16),
 text('Task 1 Title', 54, 290, 'Web Assignment', 16, '700', '#0A0A0A'),
 text('Task 1 Meta', 54, 314, 'Due today 23:59 - 2h est. - 3 subtasks', 12, '400', '#777777'),
 rect('Task 1 Badge BG', 248, 288, 56, 22, '#0A0A0A', 6, shadowEffect(0, 3, 6, '#00000025')),
 text('Task 1 Badge Text', 259, 293, 'HIGH', 10, '700', '#FFFFFF'),
 rect('Task 1 Subtrack', 54, 342, 170, 3, '#EEEEEE', 2),
 rect('Task 1 Subfill', 54, 342, 57, 3, '#0A0A0A', 2),
 text('Task 1 Subcount', 242, 338, '1/3', 11, '600', '#777777'),

 // 3D Task Card 2 (Completed Task)
 rect('Task 2 Shadow Base', 32, 380, 311, 76, '#E2E2E2', 16),
 rect('Task 2 Front', 32, 374, 311, 76, '#F8F8F8', 16, shadowEffect(0, 6, 16, '#00000008')),
 rect('Task 2 Check BG', 50, 396, 28, 28, '#0A0A0A', 14, shadowEffect(0, 3, 8, '#00000030')),
 icon('Task 2 Check', 57, 399, 14, 'check', '#FFFFFF'),
 text('Task 2 Title', 92, 394, 'Read Chapter 3', 15, '500', '#AAAAAA', { strikethrough: true }),
 text('Task 2 Meta', 92, 416, 'Completed 10:32', 12, '400', '#BBBBBB'),

 // 3D Task Card 3 (Medium Priority)
 rect('Task 3 Shadow Base', 32, 468, 311, 78, '#D8D8D8', 16),
 rect('Task 3 Front', 32, 462, 311, 78, '#FFFFFF', 16, shadowEffect(0, 8, 20, '#00000010'), {
 stroke: '#EAEAEA', strokeWidth: 1
 }),
 rect('Task 3 Priority Bar', 32, 462, 4, 78, '#888888', 16),
 text('Task 3 Title', 54, 478, 'Review lecture notes', 15, '600', '#0A0A0A'),
 text('Task 3 Meta', 54, 502, 'Due tomorrow - 45 min est.', 12, '400', '#777777'),
 rect('Task 3 Badge BG', 244, 476, 60, 22, '#F0F0F0', 6),
 text('Task 3 Badge Text', 250, 481, 'MEDIUM', 9, '700', '#555555'),

 // 3D FAB (Floating Action Button)
 rect('FAB Shadow Base', 293, 696, 56, 56, '#333333', 28),
 rect('FAB Front', 291, 690, 56, 56, '#0A0A0A', 28, shadowEffect(0, 10, 26, '#00000045')),
 icon('FAB Plus', 302, 695, 26, 'add', '#FFFFFF'),

 // Bottom Navigation with 3D top shadow
 rect('Nav Bar BG', 0, 756, 375, 56, '#FFFFFF', 0, shadowEffect(0, -6, 20, '#0000000c')),
 text('Nav Tasks', 32, 776, 'Tasks', 12, '700', '#0A0A0A'),
 text('Nav Wellbeing', 108, 776, 'Wellbeing', 12, '400', '#AAAAAA'),
 text('Nav Focus', 220, 776, 'Focus', 12, '400', '#AAAAAA'),
 text('Nav Insights', 288, 776, 'Insights', 12, '400', '#AAAAAA')
 ];

 return frame('01 - Home - Smart Todo', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 02: Add Task Bottom Sheet
// - - - - - - - - - - - - - - - -
function createScreenAddTask(x, y) {
 const children = [
 // Scrim overlay
 rect('Modal Scrim', 0, 0, 375, 812, '#0A0A0A', 32, null, { opacity: 0.4 }),

 // 3D Bottom Sheet container
 rect('Sheet Shadow Base', 0, 256, 375, 556, '#00000020', 24),
 rect('Sheet Front', 0, 250, 375, 562, '#FFFFFF', 24, shadowEffect(0, -16, 40, '#00000025')),
 rect('Sheet Handle', 164, 264, 48, 4, '#DDDDDD', 2),

 text('Sheet Title', 32, 290, 'New Task', 20, '700', '#0A0A0A'),
 icon('Close Button', 330, 288, 20, 'close', '#888888'),

 // Title input field (recessed 3D look)
 rect('Title Inset Base', 32, 332, 311, 50, '#EAEAEA', 12),
 rect('Title Field Front', 32, 330, 311, 50, '#F5F5F5', 12),
 text('Title Placeholder', 48, 345, 'Task title...', 15, '400', '#AAAAAA'),

 // Priority selector
 text('Priority Header', 32, 400, 'Priority', 13, '600', '#555555'),
 rect('Prio Low BG', 32, 424, 60, 32, '#F5F5F5', 8),
 text('Prio Low', 46, 432, 'Low', 12, '500', '#777777'),
 rect('Prio Med BG', 100, 424, 72, 32, '#F5F5F5', 8),
 text('Prio Med', 110, 432, 'Medium', 12, '500', '#777777'),
 // High selected (3D popped out)
 rect('Prio High Base', 180, 426, 60, 32, '#222222', 8),
 rect('Prio High Front', 180, 422, 60, 32, '#0A0A0A', 8, shadowEffect(0, 4, 10, '#00000030')),
 text('Prio High', 193, 430, 'High', 12, '700', '#FFFFFF'),
 rect('Prio Urg BG', 248, 424, 68, 32, '#F5F5F5', 8),
 text('Prio Urg', 259, 432, 'Urgent', 12, '500', '#777777'),

 // Deadline & Duration row
 text('Deadline Header', 32, 476, 'Deadline', 13, '600', '#555555'),
 rect('Deadline Field', 32, 500, 148, 44, '#F5F5F5', 12),
 text('Deadline Text', 48, 513, 'Oct 7, 23:59', 13, '500', '#0A0A0A'),

 text('Duration Header', 196, 476, 'Est. Duration', 13, '600', '#555555'),
 rect('Duration Field', 196, 500, 148, 44, '#F5F5F5', 12),
 text('Duration Text', 212, 513, '120 min', 13, '500', '#0A0A0A'),

 // Subtasks section
 text('Subtasks Header', 32, 564, 'Subtasks (1/3)', 13, '600', '#555555'),
 rect('Sub 1 Row', 32, 588, 311, 36, '#F8F8F8', 8),
 text('Sub 1 Text', 48, 597, 'Research references', 12, '400', '#888888', { strikethrough: true }),
 rect('Sub 2 Row', 32, 630, 311, 36, '#F0F0F0', 8),
 text('Sub 2 Text', 48, 639, 'Write the report body', 12, '600', '#0A0A0A'),
 rect('Sub 3 Row', 32, 672, 311, 36, '#F8F8F8', 8),
 text('Sub 3 Text', 48, 681, 'Add conclusion', 12, '400', '#666666'),

 // 3D Save Task Button
 rect('Save Shadow Base', 32, 746, 311, 48, '#222222', 14),
 rect('Save Button Front', 32, 740, 311, 48, '#0A0A0A', 14, shadowEffect(0, 8, 22, '#00000035')),
 text('Save Button Text', 144, 754, 'Save Task', 15, '600', '#FFFFFF')
 ];

 return frame('02 - Add Task Sheet', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 03: Digital Wellbeing
// - - - - - - - - - - - - - - - -
function createScreenWellbeing(x, y) {
 const children = [
 text('Screen Title', 32, 60, 'Wellbeing', 28, '700', '#0A0A0A'),
 text('Date Sub', 32, 94, 'Today - Wednesday, Oct 7', 13, '400', '#888888'),

 // 3D Screen Time Card
 rect('ST Shadow Base', 32, 126, 311, 104, '#262626', 20),
 rect('ST Card Front', 32, 120, 311, 104, '#0A0A0A', 20, shadowEffect(0, 14, 32, '#00000045')),
 text('ST Label', 52, 138, 'Screen Time Today', 12, '500', '#AAAAAA'),
 text('ST Value', 52, 158, '5h 18m', 36, '700', '#FFFFFF'),
 text('ST Compare', 52, 204, '+42m vs personal baseline', 12, '400', '#888888'),

 // Top Apps List
 text('Top Apps Header', 32, 246, 'Top Apps Today', 16, '700', '#0A0A0A'),

 // App 1 Card 3D
 rect('App 1 Shadow Base', 32, 276, 311, 60, '#E0E0E0', 14),
 rect('App 1 Card Front', 32, 270, 311, 60, '#F8F8F8', 14, shadowEffect(0, 6, 16, '#0000000a')),
 rect('App 1 Icon Box', 48, 284, 32, 32, '#0A0A0A', 8),
 icon('App 1 Icon', 56, 290, 16, 'play_arrow', '#FFFFFF'),
 text('App 1 Name', 92, 282, 'TikTok', 14, '700', '#0A0A0A'),
 text('App 1 Meta', 92, 302, '7 sessions - Drift detected', 12, '400', '#666666'),
 text('App 1 Duration', 264, 290, '1h 47m', 14, '700', '#0A0A0A'),

 // App 2 Card 3D
 rect('App 2 Shadow Base', 32, 348, 311, 60, '#E0E0E0', 14),
 rect('App 2 Card Front', 32, 342, 311, 60, '#F8F8F8', 14, shadowEffect(0, 6, 16, '#0000000a')),
 rect('App 2 Icon Box', 48, 356, 32, 32, '#EAEAEA', 8),
 icon('App 2 Icon', 56, 362, 16, 'photo_camera', '#0A0A0A'),
 text('App 2 Name', 92, 354, 'Instagram', 14, '600', '#0A0A0A'),
 text('App 2 Meta', 92, 374, '4 sessions - Within baseline', 12, '400', '#888888'),
 text('App 2 Duration', 276, 362, '58m', 14, '600', '#0A0A0A'),

 // Weekly Chart 3D
 text('Weekly Header', 32, 424, 'Weekly Trend (7 Days)', 16, '700', '#0A0A0A'),
 rect('Chart Shadow Base', 32, 456, 311, 140, '#E0E0E0', 18),
 rect('Chart Front', 32, 450, 311, 140, '#F8F8F8', 18, shadowEffect(0, 8, 22, '#0000000d')),

 // 3D Bars
 rect('Bar Mon Base', 56, 526, 28, 40, '#CCCCCC', 6),
 rect('Bar Mon Front', 56, 520, 28, 40, '#E0E0E0', 6),

 rect('Bar Tue Base', 96, 542, 28, 24, '#CCCCCC', 6),
 rect('Bar Tue Front', 96, 536, 28, 24, '#E0E0E0', 6),

 // Active Wednesday Bar (popped out 3D)
 rect('Bar Wed Base', 136, 492, 28, 68, '#262626', 6),
 rect('Bar Wed Front', 136, 484, 28, 68, '#0A0A0A', 6, shadowEffect(0, 6, 14, '#00000035')),

 rect('Bar Thu Base', 176, 532, 28, 34, '#CCCCCC', 6),
 rect('Bar Thu Front', 176, 526, 28, 34, '#E0E0E0', 6),

 rect('Bar Fri Base', 216, 544, 28, 22, '#CCCCCC', 6),
 rect('Bar Fri Front', 216, 538, 28, 22, '#E0E0E0', 6),

 rect('Bar Sat Base', 256, 514, 28, 52, '#CCCCCC', 6),
 rect('Bar Sat Front', 256, 508, 28, 52, '#E0E0E0', 6),

 rect('Bar Sun Base', 296, 528, 28, 38, '#CCCCCC', 6),
 rect('Bar Sun Front', 296, 522, 28, 38, '#E0E0E0', 6),

 text('Bar Day Labels', 54, 570, 'M T W T F S S', 11, '500', '#AAAAAA'),

 // Bottom Navigation
 rect('Nav Bar BG W', 0, 756, 375, 56, '#FFFFFF', 0, shadowEffect(0, -6, 20, '#0000000c')),
 text('Nav Tasks W', 32, 776, 'Tasks', 12, '400', '#AAAAAA'),
 text('Nav Wellbeing W', 104, 776, 'Wellbeing', 12, '700', '#0A0A0A'),
 text('Nav Focus W', 220, 776, 'Focus', 12, '400', '#AAAAAA'),
 text('Nav Insights W', 288, 776, 'Insights', 12, '400', '#AAAAAA')
 ];

 return frame('03 - Digital Wellbeing', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 04: Contextual Intervention (In-App)
// - - - - - - - - - - - - - - - -
function createScreenIntervention(x, y) {
 const children = [
 // Scrim overlay
 rect('Intervention Scrim', 0, 0, 375, 812, '#0A0A0A', 32, null, { opacity: 0.55 }),

 // 3D Multi-Layered Pop Card
 rect('Card Shadow Layer 3', 36, 276, 303, 330, '#111111', 24, null, { opacity: 0.35 }),
 rect('Card Shadow Layer 2', 30, 270, 315, 330, '#222222', 24, null, { opacity: 0.45 }),
 rect('Intervention Card Front', 24, 260, 327, 334, '#FFFFFF', 24, shadowEffect(0, 24, 60, '#00000060')),

 icon('Intervention Icon', 162, 284, 40, 'visibility', '#0A0A0A'),

 text('Intervention Head 1', 44, 344, 'You\'ve been scrolling', 22, '700', '#0A0A0A', { textAlign: 'center' }),
 text('Intervention Head 2', 44, 372, 'longer than usual.', 22, '700', '#0A0A0A', { textAlign: 'center' }),

 text('Intervention Body', 42, 416, 'TikTok - 47 min (+161% baseline)\nWeb Assignment (High Priority)\nmasih belum selesai.', 14, '400', '#555555', { lineHeight: 1.5, textAlign: 'center' }),

 // 3D Start Focus Button
 rect('Focus Btn Base', 44, 504, 287, 48, '#262626', 14),
 rect('Focus Btn Front', 44, 498, 287, 48, '#0A0A0A', 14, shadowEffect(0, 8, 20, '#00000035')),
 text('Focus Btn Text', 128, 512, 'Start Focus', 15, '600', '#FFFFFF'),

 // Alternative links
 text('Future View Link', 86, 560, 'Lihat dampak jika menunda', 13, '600', '#0A0A0A'),
 text('Later Dismiss', 168, 584, 'Nanti saja', 13, '400', '#999999')
 ];

 return frame('04 - Contextual Intervention', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 05: Focus Mode - Setup
// - - - - - - - - - - - - - - - -
function createScreenFocusSetup(x, y) {
 const children = [
 text('Back Link', 32, 60, 'Back', 14, '500', '#0A0A0A'),
 text('Title', 32, 92, 'Start Focus', 28, '700', '#0A0A0A'),
 text('Sub', 32, 128, 'Pilih task dan mode fokus kamu.', 14, '400', '#777777'),

 // Task selector (3D raised dark card)
 text('Task Label', 32, 168, 'Working on', 13, '600', '#555555'),
 rect('Task Sel Base', 32, 196, 311, 64, '#262626', 16),
 rect('Task Sel Front', 32, 190, 311, 64, '#0A0A0A', 16, shadowEffect(0, 10, 26, '#00000040')),
 text('Task Sel Name', 52, 204, 'Web Assignment', 16, '600', '#FFFFFF'),
 text('Task Sel Meta', 52, 228, 'Due today 23:59 - HIGH priority', 12, '400', '#AAAAAA'),
 icon('Task Sel Chevron', 300, 212, 22, 'chevron_right', '#AAAAAA'),

 // Mode Selector (3 cards: Gentle, Strict, Custom)
 text('Mode Label', 32, 276, 'Focus Mode', 13, '600', '#555555'),

 // Gentle Mode (Selected 3D)
 rect('Mode G Base', 32, 306, 95, 92, '#262626', 16),
 rect('Mode G Front', 32, 300, 95, 92, '#0A0A0A', 16, shadowEffect(0, 10, 24, '#00000045')),
 icon('Mode G Icon', 67, 314, 24, 'spa', '#FFFFFF'),
 text('Mode G Name', 48, 348, 'Gentle', 13, '600', '#FFFFFF'),
 text('Mode G Desc', 42, 370, 'Awareness', 10, '400', '#AAAAAA'),

 // Strict Mode (Unselected 3D)
 rect('Mode S Base', 140, 306, 95, 92, '#E0E0E0', 16),
 rect('Mode S Front', 140, 300, 95, 92, '#F8F8F8', 16, shadowEffect(0, 6, 16, '#0000000c')),
 icon('Mode S Icon', 175, 314, 24, 'lock', '#0A0A0A'),
 text('Mode S Name', 158, 348, 'Strict', 13, '600', '#0A0A0A'),
 text('Mode S Desc', 150, 370, 'App block', 10, '400', '#777777'),

 // Custom Mode (Unselected 3D)
 rect('Mode C Base', 248, 306, 95, 92, '#E0E0E0', 16),
 rect('Mode C Front', 248, 300, 95, 92, '#F8F8F8', 16, shadowEffect(0, 6, 16, '#0000000c')),
 icon('Mode C Icon', 283, 314, 24, 'tune', '#0A0A0A'),
 text('Mode C Name', 260, 348, 'Custom', 13, '600', '#0A0A0A'),
 text('Mode C Desc', 256, 370, 'Custom rule', 10, '400', '#777777'),

 // Duration options
 text('Dur Label', 32, 416, 'Session Duration', 13, '600', '#555555'),
 rect('Dur 25 Base', 32, 442, 68, 36, '#262626', 10),
 rect('Dur 25 Front', 32, 438, 68, 36, '#0A0A0A', 10, shadowEffect(0, 6, 14, '#00000030')),
 text('Dur 25 Text', 52, 448, '25m', 13, '600', '#FFFFFF'),

 rect('Dur 45 Front', 108, 438, 68, 36, '#F5F5F5', 10),
 text('Dur 45 Text', 128, 448, '45m', 13, '600', '#0A0A0A'),

 rect('Dur 60 Front', 184, 438, 68, 36, '#F5F5F5', 10),
 text('Dur 60 Text', 204, 448, '60m', 13, '600', '#0A0A0A'),

 rect('Dur Cust Front', 260, 438, 83, 36, '#F5F5F5', 10),
 text('Dur Cust Text', 274, 448, 'Custom', 13, '600', '#0A0A0A'),

 // 3D Begin Focus Button
 rect('Begin Base', 32, 718, 311, 56, '#262626', 16),
 rect('Begin Front', 32, 712, 311, 56, '#0A0A0A', 16, shadowEffect(0, 10, 26, '#00000040')),
 text('Begin Text', 122, 730, 'Begin Focus', 16, '600', '#FFFFFF')
 ];

 return frame('05 - Focus Setup', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 06: Focus Mode - Active (Dark 3D)
// - - - - - - - - - - - - - - - -
function createScreenFocusActive(x, y) {
 const children = [
 text('Focus Tag', 32, 60, 'FOCUSING', 11, '700', '#888888', { letterSpacing: 3 }),
 text('Task Name', 32, 86, 'Web Assignment', 26, '700', '#FFFFFF'),
 text('Current Step', 32, 122, 'Current: Write the report body', 14, '400', '#AAAAAA'),

 // 3D Ring Timer with glow effect
 ellipse('Timer Glow Outer', 60, 174, 255, 255, '#1A1A1A', shadowEffect(0, 16, 40, '#FFFFFF18')),
 ellipse('Timer Ring Base', 66, 180, 243, 243, '#141414', null, {
 stroke: '#333333', strokeWidth: 10
 }),
 ellipse('Timer Ring Active', 66, 180, 243, 243, 'transparent', shadowEffect(0, 0, 16, '#FFFFFF40'), {
 stroke: '#FFFFFF', strokeWidth: 10, sweepAngle: 240
 }),
 text('Timer Digits', 126, 276, '18:32', 50, '700', '#FFFFFF'),
 text('Timer Sub', 148, 342, 'remaining', 13, '400', '#777777'),

 // Subtasks In-Focus
 text('Subtasks Header', 32, 456, 'Subtasks', 13, '600', '#888888'),

 // Sub 1 Done
 rect('Sub 1 Card', 32, 480, 311, 40, '#1A1A1A', 10),
 icon('Sub 1 Check', 48, 490, 14, 'check', '#FFFFFF'),
 text('Sub 1 Text', 76, 494, 'Research references', 13, '400', '#777777', { strikethrough: true }),

 // Sub 2 Active (Raised 3D)
 rect('Sub 2 Base', 32, 530, 311, 42, '#111111', 10),
 rect('Sub 2 Front', 32, 526, 311, 42, '#2A2A2A', 10, shadowEffect(0, 6, 16, '#00000060')),
 rect('Sub 2 Dot', 46, 538, 18, 18, 'transparent', 9, null, { stroke: '#FFFFFF', strokeWidth: 2 }),
 text('Sub 2 Text', 76, 538, 'Write the report body', 13, '600', '#FFFFFF'),

 // Sub 3 Pending
 rect('Sub 3 Card', 32, 578, 311, 40, '#141414', 10),
 rect('Sub 3 Dot', 46, 590, 18, 18, 'transparent', 9, null, { stroke: '#444444', strokeWidth: 2 }),
 text('Sub 3 Text', 76, 590, 'Add conclusion', 13, '400', '#555555'),

 // 3D Controls Row
 rect('Pause Base', 32, 680, 140, 52, '#111111', 14),
 rect('Pause Front', 32, 674, 140, 52, '#1F1F1F', 14, shadowEffect(0, 8, 20, '#00000060')),
 text('Pause Text', 68, 692, 'Pause', 15, '600', '#AAAAAA'),

 rect('Finish Base', 203, 680, 140, 52, '#D0D0D0', 14),
 rect('Finish Front', 203, 674, 140, 52, '#FFFFFF', 14, shadowEffect(0, 10, 26, '#FFFFFF25')),
 text('Finish Text', 236, 692, 'Finish', 15, '700', '#0A0A0A'),

 text('Emergency Link', 124, 754, 'Emergency exit', 12, '400', '#666666')
 ];

 return frame('06 - Focus Mode - Active', x, y, 375, 812, '#0A0A0A', 32, children, shadowEffect(0, 20, 50, '#00000050'));
}

// - - - - - - - - - - - - - - - -
// Screen 07: Future View + Domino Effect
// - - - - - - - - - - - - - - - -
function createScreenFutureView(x, y) {
 const children = [
 text('Back Link', 32, 60, 'Back', 14, '500', '#0A0A0A'),
 text('Title', 32, 92, 'What happens next?', 26, '700', '#0A0A0A'),
 text('Disclaimer', 32, 130, 'Berdasarkan jadwalmu saat ini - proyeksi,\nbukan ramalan pasti.', 13, '400', '#777777', { lineHeight: 1.4 }),

 // Scenario A: Start Now (Dark 3D card)
 rect('Scen A Base', 32, 184, 148, 260, '#262626', 20),
 rect('Scen A Front', 32, 178, 148, 260, '#0A0A0A', 20, shadowEffect(0, 14, 34, '#00000045')),
 text('Scen A Tag', 48, 194, 'IF I START NOW', 9, '700', '#AAAAAA', { letterSpacing: 2 }),
 text('Scen A T1', 48, 222, '15:10\nStart right now\n\n15:10-17:10\nFocus session\n\n17:10\nTask done \n\nEvening\nFree', 12, '400', '#FFFFFF', { lineHeight: 1.6 }),

 // Scenario B: Keep Delaying (Light 3D card)
 rect('Scen B Base', 196, 184, 148, 260, '#E0E0E0', 20),
 rect('Scen B Front', 196, 178, 148, 260, '#F8F8F8', 20, shadowEffect(0, 8, 22, '#0000000f')),
 text('Scen B Tag', 208, 194, 'IF I DELAY', 9, '700', '#AAAAAA', { letterSpacing: 2 }),
 text('Scen B T1', 208, 222, '20:00\nStart late\n\n20:00-22:00\nWork at night\n\n22:00+\nLess free time\n\n23:59\nDeadline', 12, '400', '#555555', { lineHeight: 1.6 }),

 // Domino Effect Section
 text('Domino Header', 32, 460, 'Domino Effect', 16, '700', '#0A0A0A'),
 text('Domino Sub', 32, 484, '1 task may affect 3 upcoming tasks.', 13, '400', '#777777'),

 // Domino Node 1
 rect('Node 1 Base', 32, 514, 311, 40, '#262626', 10),
 rect('Node 1 Front', 32, 510, 311, 40, '#0A0A0A', 10, shadowEffect(0, 4, 12, '#00000030')),
 text('Node 1 Text', 48, 522, 'Web Assignment (CURRENT)', 13, '600', '#FFFFFF'),
 icon('Arrow 1', 180, 552, 18, 'arrow_downward', '#AAAAAA'),

 // Domino Node 2
 rect('Node 2 Base', 32, 574, 311, 36, '#E0E0E0', 10),
 rect('Node 2 Front', 32, 570, 311, 36, '#F8F8F8', 10, shadowEffect(0, 4, 10, '#0000000a')),
 text('Node 2 Text', 48, 580, 'Peer review submission', 13, '400', '#555555'),
 icon('Arrow 2', 180, 608, 18, 'arrow_downward', '#AAAAAA'),

 // Domino Node 3
 rect('Node 3 Base', 32, 630, 311, 36, '#E0E0E0', 10),
 rect('Node 3 Front', 32, 626, 311, 36, '#F8F8F8', 10, shadowEffect(0, 4, 10, '#0000000a')),
 text('Node 3 Text', 48, 636, 'Final presentation', 13, '400', '#555555'),

 // 3D Start Focus Now Button
 rect('SF Base', 32, 718, 311, 52, '#262626', 16),
 rect('SF Front', 32, 712, 311, 52, '#0A0A0A', 16, shadowEffect(0, 10, 26, '#00000040')),
 text('SF Text', 104, 728, 'Start Focus Now', 16, '600', '#FFFFFF')
 ];

 return frame('07 - Future View', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 08: Insights & Daily Reflection
// - - - - - - - - - - - - - - - -
function createScreenInsights(x, y) {
 const children = [
 text('Title', 32, 60, 'Insights', 28, '700', '#0A0A0A'),
 text('Date', 32, 98, 'October 7 - Daily Reflection', 14, '400', '#777777'),

 // 3D Stats Row Card
 rect('Stats Base', 32, 132, 311, 88, '#E0E0E0', 20),
 rect('Stats Front', 32, 126, 311, 88, '#F8F8F8', 20, shadowEffect(0, 8, 22, '#0000000d')),
 text('S1 Val', 50, 146, '2/3', 24, '700', '#0A0A0A'),
 text('S1 Lbl', 50, 178, 'Tasks done', 11, '500', '#777777'),
 rect('DiV 1', 144, 142, 1, 56, '#E0E0E0'),
 text('S2 Val', 164, 146, '1h 42m', 22, '700', '#0A0A0A'),
 text('S2 Lbl', 164, 178, 'Focus time', 11, '500', '#777777'),
 rect('DiV 2', 260, 142, 1, 56, '#E0E0E0'),
 text('S3 Val', 276, 146, '5h 18m', 18, '700', '#0A0A0A'),
 text('S3 Lbl', 276, 176, 'Screen time', 11, '500', '#777777'),

 // Longest Distraction
 rect('Dist Base', 32, 234, 311, 52, '#E0E0E0', 12),
 rect('Dist Front', 32, 228, 311, 52, '#F8F8F8', 12, shadowEffect(0, 6, 16, '#0000000a')),
 text('Dist Lbl', 50, 240, 'Longest distraction session', 12, '500', '#777777'),
 text('Dist Val', 50, 258, 'TikTok - 45 min', 14, '600', '#0A0A0A'),

 // 3D Insight Box (Dark Popped Out)
 rect('Insight Base', 32, 302, 311, 96, '#262626', 20),
 rect('Insight Front', 32, 296, 311, 96, '#0A0A0A', 20, shadowEffect(0, 12, 30, '#00000045')),
 text('Insight Tag', 52, 312, 'INSIGHT', 10, '700', '#888888', { letterSpacing: 2 }),
 text('Insight Quote', 52, 332, '"You completed difficult tasks earlier\nthan usual today."', 14, '500', '#FFFFFF', { lineHeight: 1.5 }),

 // 3D Suggestion Box (Light Popped Out)
 rect('Sugg Base', 32, 414, 311, 96, '#E0E0E0', 20),
 rect('Sugg Front', 32, 408, 311, 96, '#F8F8F8', 20, shadowEffect(0, 8, 22, '#0000000d')),
 text('Sugg Tag', 52, 424, 'SUGGESTION', 10, '700', '#888888', { letterSpacing: 2 }),
 text('Sugg Quote', 52, 444, '"Tomorrow, start your hardest task\nwithin your first productive window."', 14, '500', '#0A0A0A', { lineHeight: 1.5 }),

 text('AI Attribution', 52, 514, 'Generated by Anchor Local AI', 11, '400', '#AAAAAA'),

 // Pattern Recognition
 text('Pattern Header', 32, 546, 'Pattern Recognition', 16, '700', '#0A0A0A'),
 rect('P1 Card', 32, 574, 311, 44, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('P1 Text', 48, 588, 'Most productive: 09:00 - 11:00', 13, '500', '#0A0A0A'),
 rect('P2 Card', 32, 626, 311, 44, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('P2 Text', 48, 640, 'Peak distraction: 14:00 - 16:00', 13, '500', '#0A0A0A'),

 // Bottom Navigation
 rect('Nav Bar BG I', 0, 756, 375, 56, '#FFFFFF', 0, shadowEffect(0, -6, 20, '#0000000c')),
 text('Nav Tasks I', 32, 776, 'Tasks', 12, '400', '#AAAAAA'),
 text('Nav Wellbeing I', 104, 776, 'Wellbeing', 12, '400', '#AAAAAA'),
 text('Nav Focus I', 220, 776, 'Focus', 12, '400', '#AAAAAA'),
 text('Nav Insights I', 288, 776, 'Insights', 12, '700', '#0A0A0A')
 ];

 return frame('08 - Insights - Daily Reflection', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 09: Settings & Privacy
// - - - - - - - - - - - - - - - -
function createScreenSettings(x, y) {
 const children = [
 text('Back Link', 32, 60, 'Back', 14, '500', '#0A0A0A'),
 text('Title', 32, 92, 'Settings', 28, '700', '#0A0A0A'),

 // Permissions section
 text('Sec Perm', 32, 140, 'PERMISSIONS', 11, '700', '#888888', { letterSpacing: 2 }),
 rect('P1 Card', 32, 164, 311, 52, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('P1 Title', 48, 176, 'Usage Access', 14, '600', '#0A0A0A'),
 text('P1 Sub', 48, 196, 'Granted', 12, '400', '#555555'),
 ellipse('P1 Indicator', 312, 184, 12, 12, '#0A0A0A', shadowEffect(0, 2, 6, '#00000030')),

 rect('P2 Card', 32, 224, 311, 52, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('P2 Title', 48, 236, 'Notifications', 14, '600', '#0A0A0A'),
 text('P2 Sub', 48, 256, 'Belum aktif - ketuk untuk izinkan', 12, '400', '#999999'),
 ellipse('P2 Indicator', 312, 244, 12, 12, '#DDDDDD'),

 // Privacy & AI
 text('Sec Privacy', 32, 296, 'PRIVACY & AI', 11, '700', '#888888', { letterSpacing: 2 }),
 rect('AI Card', 32, 320, 311, 54, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('AI Title', 48, 332, 'AI Personal Insights', 14, '600', '#0A0A0A'),
 text('AI Sub', 48, 352, 'Hanya kirim ringkasan agregat', 12, '400', '#777777'),
 rect('Toggle Track', 294, 336, 38, 22, '#EAEAEA', 11),
 ellipse('Toggle Thumb', 296, 338, 18, 18, '#FFFFFF', shadowEffect(0, 2, 6, '#00000025')),

 // Data section
 text('Sec Data', 32, 394, 'DATA CONTROLS', 11, '700', '#888888', { letterSpacing: 2 }),
 rect('Ret Card', 32, 418, 311, 52, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('Ret Title', 48, 430, 'Data Retention', 14, '600', '#0A0A0A'),
 text('Ret Sub', 48, 450, 'Simpan maksimal 30 hari', 12, '400', '#777777'),
 icon('Ret Chevron', 322, 436, 22, 'chevron_right', '#AAAAAA'),

 rect('Del Card', 32, 478, 311, 52, '#FFF5F5', 12, shadowEffect(0, 4, 12, '#CC333315')),
 text('Del Title', 48, 496, 'Hapus Semua Data', 14, '600', '#CC3333'),

 // 3D Privacy Assurance Card
 rect('Assure Base', 32, 552, 311, 64, '#262626', 16),
 rect('Assure Front', 32, 546, 311, 64, '#0A0A0A', 16, shadowEffect(0, 10, 24, '#00000040')),
 text('Assure Text', 50, 562, 'Raw usage data tidak pernah meninggalkan HP.\nAI hanya menerima data agregat.', 12, '400', '#CCCCCC', { lineHeight: 1.5 })
 ];

 return frame('09 - Settings & Privacy', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Screen 10: Task Detail Screen
// - - - - - - - - - - - - - - - -
function createScreenTaskDetail(x, y) {
 const children = [
 text('Back Link', 32, 60, 'Tasks', 14, '500', '#0A0A0A'),
 text('Edit Link', 330, 60, 'Edit', 14, '600', '#0A0A0A'),

 rect('Priority Bar Top', 32, 94, 311, 4, '#0A0A0A', 2),
 text('Task Title', 32, 114, 'Web Assignment', 26, '700', '#0A0A0A'),

 // Badges 3D
 rect('Badge High Base', 32, 158, 56, 24, '#262626', 6),
 rect('Badge High Front', 32, 154, 56, 24, '#0A0A0A', 6, shadowEffect(0, 3, 8, '#00000025')),
 text('Badge High Text', 44, 160, 'HIGH', 10, '700', '#FFFFFF'),

 rect('Badge Status', 96, 154, 94, 24, '#F0F0F0', 6),
 text('Badge Status Text', 105, 160, 'IN PROGRESS', 9, '700', '#555555'),

 // Task Metadata
 text('Meta Due', 32, 196, 'Due Oct 7, 23:59', 13, '500', '#555555'),
 text('Meta Est', 32, 220, 'Estimated: 2 hours', 13, '500', '#555555'),
 text('Meta Prog', 32, 244, 'Progress: 33% (1 of 3 steps)', 13, '500', '#555555'),

 rect('Prog Track TD', 32, 268, 311, 6, '#EEEEEE', 3),
 rect('Prog Fill TD', 32, 268, 103, 6, '#0A0A0A', 3, shadowEffect(0, 2, 6, '#00000025')),

 // Subtasks Checklist
 text('Subtasks Header', 32, 296, 'Subtasks', 16, '700', '#0A0A0A'),

 rect('ST 1 Card', 32, 324, 311, 44, '#F8F8F8', 10, shadowEffect(0, 4, 12, '#00000008')),
 rect('ST 1 Check BG', 48, 338, 18, 18, '#0A0A0A', 9, shadowEffect(0, 2, 6, '#00000025')),
 icon('ST 1 Check', 53, 341, 10, 'check', '#FFFFFF'),
 text('ST 1 Text', 78, 342, 'Research references', 13, '400', '#888888', { strikethrough: true }),

 rect('ST 2 Base', 32, 380, 311, 46, '#E0E0E0', 10),
 rect('ST 2 Front', 32, 376, 311, 46, '#FFFFFF', 10, shadowEffect(0, 6, 16, '#00000010'), {
 stroke: '#0A0A0A', strokeWidth: 1.5
 }),
 rect('ST 2 Circle', 48, 390, 18, 18, 'transparent', 9, null, { stroke: '#0A0A0A', strokeWidth: 2 }),
 text('ST 2 Text', 78, 392, 'Write the report body (Active)', 13, '700', '#0A0A0A'),

 rect('ST 3 Card', 32, 432, 311, 44, '#F8F8F8', 10, shadowEffect(0, 4, 12, '#00000008')),
 rect('ST 3 Circle', 48, 446, 18, 18, 'transparent', 9, null, { stroke: '#CCCCCC', strokeWidth: 2 }),
 text('ST 3 Text', 78, 448, 'Add conclusion', 13, '400', '#777777'),

 // Future View Callout (3D)
 rect('FV Callout Base', 32, 508, 311, 52, '#E0E0E0', 14),
 rect('FV Callout Front', 32, 502, 311, 52, '#F8F8F8', 14, shadowEffect(0, 8, 20, '#0000000f')),
 text('FV Callout Text', 54, 520, 'Lihat skenario jika ditunda', 13, '600', '#0A0A0A'),

 // 3D Start Focus Action
 rect('SF TD Base', 32, 686, 311, 52, '#262626', 16),
 rect('SF TD Front', 32, 680, 311, 52, '#0A0A0A', 16, shadowEffect(0, 10, 26, '#00000040')),
 text('SF TD Text', 96, 696, 'Start Focus Session', 15, '600', '#FFFFFF'),

 // Reschedule & Done buttons
 rect('Resched Front', 32, 742, 148, 40, '#F5F5F5', 12, shadowEffect(0, 4, 10, '#0000000a')),
 text('Resched Text', 56, 754, 'Reschedule', 13, '500', '#555555'),

 rect('Done Front', 195, 742, 148, 40, '#F5F5F5', 12, shadowEffect(0, 4, 10, '#0000000a')),
 text('Done Text', 216, 754, 'Mark as Done', 13, '600', '#0A0A0A')
 ];

 return frame('10 - Task Detail', x, y, 375, 812, '#FFFFFF', 32, children, shadowEffect(0, 20, 50, '#00000018'));
}

// - - - - - - - - - - - - - - - -
// Top Header & 3D Hero Showcase
// - - - - - - - - - - - - - - - -
function createHeaderBanner() {
 const children = [
 text('Title', 48, 20, 'Anchor - Personal Focus Assistant', 22, '700', '#FFFFFF'),
 text('Subtitle', 48, 50, 'Black & White Minimalist - 3D Depth System - Android Mobile - PRD v0.1', 12, '400', '#777777')
 ];
 return frame('Anchor - Top Banner', 0, 0, 1680, 80, '#0A0A0A', 0, children);
}

function createHeroShowcase() {
 const children = [
 text('Hero Title', 60, 48, 'Anchor', 68, '700', '#FFFFFF', { letterSpacing: -3 }),
 text('Hero Slogan', 60, 126, 'Stay anchored to\nwhat matters.', 28, '400', '#888888', { lineHeight: 1.3 }),
 text('Hero Sub', 60, 212, 'Personal Focus Assistant - Android - Local-first & Privacy-preserving', 14, '400', '#555555'),

 // 3D Tag Pills
 rect('Pill 1 Base', 60, 264, 140, 36, '#000000', 18),
 rect('Pill 1 Front', 60, 260, 140, 36, '#1C1C1C', 18, shadowEffect(0, 6, 14, '#00000050')),
 text('Pill 1 Text', 78, 272, 'Local-first', 12, '600', '#FFFFFF'),

 rect('Pill 2 Base', 212, 264, 150, 36, '#000000', 18),
 rect('Pill 2 Front', 212, 260, 150, 36, '#1C1C1C', 18, shadowEffect(0, 6, 14, '#00000050')),
 text('Pill 2 Text', 230, 272, 'Privacy-first', 12, '600', '#FFFFFF'),

 rect('Pill 3 Base', 374, 264, 150, 36, '#000000', 18),
 rect('Pill 3 Front', 374, 260, 150, 36, '#1C1C1C', 18, shadowEffect(0, 6, 14, '#00000050')),
 text('Pill 3 Text', 392, 272, 'AI Optional', 12, '600', '#FFFFFF'),

 // 3D Metric Stat Cards
 rect('Stat 1 Base', 60, 340, 140, 84, '#000000', 18),
 rect('Stat 1 Front', 60, 334, 140, 84, '#181818', 18, shadowEffect(0, 10, 24, '#00000060')),
 text('Stat 1 Val', 80, 354, '5', 36, '700', '#FFFFFF'),
 text('Stat 1 Lbl', 80, 396, 'Phases in MVP', 11, '500', '#777777'),

 rect('Stat 2 Base', 212, 340, 140, 84, '#000000', 18),
 rect('Stat 2 Front', 212, 334, 140, 84, '#181818', 18, shadowEffect(0, 10, 24, '#00000060')),
 text('Stat 2 Val', 232, 354, '10', 36, '700', '#FFFFFF'),
 text('Stat 2 Lbl', 232, 396, 'Screens Designed', 11, '500', '#777777'),

 rect('Stat 3 Base', 364, 340, 140, 84, '#CCCCCC', 18),
 rect('Stat 3 Front', 364, 334, 140, 84, '#FFFFFF', 18, shadowEffect(0, 10, 26, '#FFFFFF20')),
 text('Stat 3 Val', 384, 354, '3D', 36, '700', '#0A0A0A'),
 text('Stat 3 Lbl', 384, 396, 'Depth System', 11, '600', '#666666'),

 // - 3D Layered Smartphone Mockup in Perspective -
 // Background Phone Shadow Plate (Far depth)
 rect('Phone Shadow Plate 3', 740, 60, 280, 480, '#111111', 44, null, { opacity: 0.4 }),
 // Background Phone Shadow Plate (Mid depth)
 rect('Phone Shadow Plate 2', 820, 50, 280, 480, '#1E1E1E', 44, shadowEffect(-12, 20, 40, '#00000060')),

 // Main 3D Phone Body
 rect('Phone Bezel Outer Base', 930, 38, 286, 508, '#050505', 46),
 rect('Phone Bezel Outer', 924, 32, 286, 508, '#1C1C1C', 46, shadowEffect(0, 30, 70, '#00000085')),
 rect('Phone Screen Glass', 932, 44, 270, 484, '#FFFFFF', 38),

 // Phone Notch
 rect('Phone Notch Bar', 1032, 48, 70, 12, '#1C1C1C', 6),

 // In-Phone Mini UI (Home Preview)
 text('Mini Greeting', 952, 76, 'Good morning', 10, '500', '#888888'),
 text('Mini Date', 952, 90, 'Wednesday, Oct 7', 16, '700', '#0A0A0A'),

 rect('Mini Card Base', 952, 118, 230, 68, '#262626', 14),
 rect('Mini Card Front', 952, 114, 230, 68, '#0A0A0A', 14, shadowEffect(0, 8, 18, '#00000040')),
 text('Mini Card Sub', 966, 126, 'Today\'s progress', 9, '500', '#AAAAAA'),
 text('Mini Card Done', 966, 142, '1 of 3 completed', 13, '700', '#FFFFFF'),
 rect('Mini Card Track', 966, 164, 140, 3, '#333333', 2),
 rect('Mini Card Fill', 966, 164, 46, 3, '#FFFFFF', 2),
 text('Mini Card Pct', 1124, 138, '33%', 18, '700', '#FFFFFF'),

 // Task Item 1 3D
 rect('Mini T1 Base', 952, 198, 230, 64, '#E0E0E0', 12),
 rect('Mini T1 Front', 952, 194, 230, 64, '#FFFFFF', 12, shadowEffect(0, 6, 16, '#00000010'), {
 stroke: '#EAEAEA', strokeWidth: 1
 }),
 rect('Mini T1 Bar', 952, 194, 3, 64, '#0A0A0A', 12),
 text('Mini T1 Name', 966, 206, 'Web Assignment', 12, '700', '#0A0A0A'),
 text('Mini T1 Meta', 966, 222, 'Due today - 2h est.', 9, '400', '#777777'),
 rect('Mini T1 Badge', 1130, 204, 42, 18, '#0A0A0A', 5),
 text('Mini T1 Badge Text', 1138, 208, 'HIGH', 8, '700', '#FFFFFF'),

 // Task Item 2 Done
 rect('Mini T2 Front', 952, 268, 230, 52, '#F8F8F8', 12, shadowEffect(0, 4, 12, '#00000008')),
 text('Mini T2 Text', 966, 286, 'Read Chapter 3', 11, '400', '#AAAAAA', { strikethrough: true }),

 // Task Item 3
 rect('Mini T3 Base', 952, 332, 230, 56, '#E0E0E0', 12),
 rect('Mini T3 Front', 952, 328, 230, 56, '#FFFFFF', 12, shadowEffect(0, 6, 14, '#0000000e'), {
 stroke: '#EAEAEA', strokeWidth: 1
 }),
 rect('Mini T3 Bar', 952, 328, 3, 56, '#888888', 12),
 text('Mini T3 Name', 966, 340, 'Review lecture notes', 11, '600', '#0A0A0A'),
 text('Mini T3 Meta', 966, 356, 'Due tomorrow - 45m', 9, '400', '#777777'),

 // FAB Button 3D
 rect('Mini FAB Base', 1128, 412, 44, 44, '#262626', 22),
 rect('Mini FAB Front', 1126, 408, 44, 44, '#0A0A0A', 22, shadowEffect(0, 8, 18, '#00000045')),
 icon('Mini FAB Plus', 1136, 414, 20, 'add', '#FFFFFF'),

 // Mini Bottom NaV rect('Mini NaV BG', 932, 486, 270, 42, '#FFFFFF', 0, shadowEffect(0, -4, 12, '#00000008')),
 text('Mini NaV Text', 950, 502, 'Tasks Wellbeing Focus Insights', 8, '600', '#888888')
 ];

 return frame('3D Hero Showcase', 0, 100, 1680, 580, '#0A0A0A', 0, children);
}

// - - - - - - - - - - - - - - - -
// Assembly & Output
// - - - - - - - - - - - - - - - -
function buildFullDocument() {
 const children = [
 createHeaderBanner(),
 createHeroShowcase(),

 // Row 1 (y = 720)
 createScreenSplash(0, 720),
 createScreenPermissions(420, 720),
 createScreenHome(840, 720),
 createScreenAddTask(1260, 720),

 // Row 2 (y = 1600)
 createScreenWellbeing(0, 1600),
 createScreenIntervention(420, 1600),
 createScreenFocusSetup(840, 1600),
 createScreenFocusActive(1260, 1600),

 // Row 3 (y = 2480)
 createScreenFutureView(0, 2480),
 createScreenInsights(420, 2480),
 createScreenSettings(840, 2480),
 createScreenTaskDetail(1260, 2480)
 ];

 return {
 version: '2.19',
 fileToken: 'd5ad0ffc-ae13-40b2-b2f2-8e4cffeb9fb8',
 children: children
 };
}

const doc = buildFullDocument();
const outputPath = path.resolve('d:/Perkuliahan/project/reminder/docs/superpowers/design/gamabaran.pen');
fs.writeFileSync(outputPath, JSON.stringify(doc, null, 2), 'utf-8');
console.log('Successfully generated valid pen.deV file at:', outputPath);
console.log('Total children top-level:', doc.children.length);
