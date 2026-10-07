import fs from 'node:fs/promises'
import { join } from 'node:path'
import { worktreeDir } from './config.js'
import { cd, step, success } from './lib.js'

const TARGET_PACKAGE = 'ua.entaytion.entinygram'

const GOOGLE_SERVICES_FILES = [
  'TMessagesProj/google-services.json',
  'TMessagesProj_App/google-services.json',
]

async function ensureGoogleServicesBranding() {
  const repo = cd(worktreeDir)
  let missing = 0

  for (const file of GOOGLE_SERVICES_FILES) {
    const absPath = join(worktreeDir, file)
    try {
      const stat = await fs.stat(absPath).catch(() => null)
      if (!stat || !stat.isFile()) {
        missing++
        console.warn(`Warning: ${file} is missing; run bun run setup to provision the Firebase client.`)
        continue
      }

      const config = JSON.parse(await fs.readFile(absPath, 'utf8'))
      if (!config.client?.some((client: { client_info?: { android_client_info?: { package_name?: string } } }) =>
        client.client_info?.android_client_info?.package_name === TARGET_PACKAGE)) {
        missing++
        console.warn(`Warning: ${file} has no registered Android client for "${TARGET_PACKAGE}".`)
        continue
      }

      const gitPath = file.replaceAll('\\', '/')
      await repo`git update-index --skip-worktree ${gitPath}`
    } catch (err) {
      missing++
      console.warn(`Warning: Could not process ${file}: ${err}`)
    }
  }

  if (missing === 0) {
    success('All google-services.json files are branded and marked skip-worktree.')
  } else {
    throw new Error(`${missing} google-services.json file(s) need a registered Firebase Android client.`)
  }
}

async function main() {
  step('Applying entinyGram branding...')
  await ensureGoogleServicesBranding()
  success('Branding applied successfully.')
}

main().catch((err) => {
  console.error('Error applying branding:', err)
  process.exit(1)
})
