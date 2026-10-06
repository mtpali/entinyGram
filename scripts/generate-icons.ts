import fs from 'node:fs/promises'
import { dirname, join } from 'node:path'
import sharp from 'sharp'
import { rootDir } from './config.js'
import { ensureDir, step, success } from './lib.js'

const source = join(rootDir, 'src/res/launcher/nagramxf')
const generated = 'src/res/launcher/generated'
const densities = { mdpi: 1, hdpi: 1.5, xhdpi: 2, xxhdpi: 3, xxxhdpi: 4 }

async function writeGenerated(path: string, content: string | Buffer) {
  const target = join(rootDir, generated, path)
  await ensureDir(dirname(target))
  const bytes = typeof content === 'string' ? Buffer.from(content) : content
  const existing = await fs.readFile(target).catch(() => null)
  if (existing?.equals(bytes)) return
  step(`Generating ${path}`)
  await fs.writeFile(target, bytes)
}

function bitmap(resource: string) {
  return `<?xml version="1.0" encoding="utf-8"?>
<bitmap xmlns:android="http://schemas.android.com/apk/res/android"
    android:src="${resource}"
    android:gravity="fill" />
`
}

const adaptive = `<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/icon_background_inu" />
    <foreground android:drawable="@drawable/icon_foreground_inu" />
    <monochrome android:drawable="@drawable/icon_plane_inu" />
</adaptive-icon>
`

const background = `<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FF000000" />
</shape>
`

for (const [density, scale] of Object.entries(densities)) {
  const legacy = await sharp(join(source, 'icon.png')).resize(48 * scale, 48 * scale).png().toBuffer()
  const foreground = await sharp(join(source, 'foreground.png')).resize(108 * scale, 108 * scale).png().toBuffer()
  const notification = await sharp(join(source, 'foreground.png')).trim().resize(24 * scale, 24 * scale, {
    fit: 'contain', background: { r: 255, g: 255, b: 255, alpha: 0 },
  }).png().toBuffer()
  await writeGenerated(`mipmap-${density}/ic_launcher.png`, legacy)
  await writeGenerated(`mipmap-${density}/ic_launcher_round.png`, legacy)
  await writeGenerated(`mipmap-${density}/telegram_foreground.png`, foreground)
  await writeGenerated(`drawable-${density}/telegram_notification.png`, notification)
}

for (const name of ['icon_foreground_inu', 'icon_foreground_inu_round', 'icon_foreground_inu_debug', 'icon_plane_inu']) {
  await writeGenerated(`drawable/${name}.xml`, bitmap('@mipmap/telegram_foreground'))
}
for (const name of ['icon_settings_inu', 'icon_notification_inu']) {
  await writeGenerated(`drawable/${name}.xml`, bitmap('@drawable/telegram_notification'))
}
await writeGenerated('drawable/icon_background_inu.xml', background)
for (const dir of ['mipmap', 'mipmap-debug']) {
  for (const name of ['ic_launcher', 'ic_launcher_round']) {
    await writeGenerated(`${dir}/${name}.xml`, adaptive)
  }
}
success('NagramXF launcher and notification icons generated')
