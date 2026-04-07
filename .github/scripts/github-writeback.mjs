#!/usr/bin/env node

import fs from 'node:fs/promises';

const token = process.env.GITHUB_TOKEN;

if (!token) {
  throw new Error('GITHUB_TOKEN environment variable is required');
}

function usage() {
  console.error(
    [
      'Usage:',
      '  node .github/scripts/github-writeback.mjs pr-review --repo owner/repo --number 123 --body-file /tmp/review.md [--commit-sha abc123]',
      '  node .github/scripts/github-writeback.mjs issue-comment --repo owner/repo --number 88 --body-file /tmp/comment.md',
    ].join('\n'),
  );
  process.exit(1);
}

function parseArgs(argv) {
  const [command, ...rest] = argv;
  if (!command) usage();

  const options = { command };
  for (let i = 0; i < rest.length; i += 1) {
    const key = rest[i];
    const value = rest[i + 1];

    if (!key.startsWith('--') || value === undefined) usage();
    options[key.slice(2)] = value;
    i += 1;
  }

  return options;
}

async function githubRequest(path, init = {}) {
  const response = await fetch(`https://api.github.com${path}`, {
    ...init,
    headers: {
      Accept: 'application/vnd.github+json',
      Authorization: `Bearer ${token}`,
      'User-Agent': 'frog-github-writeback',
      'Content-Type': 'application/json',
      ...(init.headers ?? {}),
    },
  });

  if (!response.ok) {
    const body = await response.text();
    throw new Error(`GitHub API ${path} failed: ${response.status} ${body}`);
  }

  const text = await response.text();
  return text ? JSON.parse(text) : null;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  const repo = args.repo;
  const number = args.number;
  const bodyFile = args['body-file'];

  if (!repo || !number || !bodyFile) usage();

  const [owner, name] = repo.split('/');
  if (!owner || !name) {
    throw new Error(`Invalid repo: ${repo}`);
  }

  const body = (await fs.readFile(bodyFile, 'utf8')).trim();
  if (!body) {
    throw new Error('Body file is empty');
  }

  if (args.command === 'pr-review') {
    const payload = {
      event: 'COMMENT',
      body,
    };

    if (args['commit-sha']) {
      payload.commit_id = args['commit-sha'];
    }

    const review = await githubRequest(`/repos/${owner}/${name}/pulls/${number}/reviews`, {
      method: 'POST',
      body: JSON.stringify(payload),
    });

    console.log(
      JSON.stringify(
        {
          ok: true,
          type: 'pr-review',
          repo,
          number,
          review_id: review?.id ?? null,
          html_url: review?.html_url ?? null,
        },
        null,
        2,
      ),
    );
    return;
  }

  if (args.command === 'issue-comment') {
    const comment = await githubRequest(`/repos/${owner}/${name}/issues/${number}/comments`, {
      method: 'POST',
      body: JSON.stringify({ body }),
    });

    console.log(
      JSON.stringify(
        {
          ok: true,
          type: 'issue-comment',
          repo,
          number,
          comment_id: comment?.id ?? null,
          html_url: comment?.html_url ?? null,
        },
        null,
        2,
      ),
    );
    return;
  }

  usage();
}

await main();
