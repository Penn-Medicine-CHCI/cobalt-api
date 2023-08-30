import gulp from 'gulp';
import plugins from 'gulp-load-plugins';
import browser from 'browser-sync';
import rimraf from 'rimraf';
import panini from 'panini';
import yargs from 'yargs';
import lazypipe from 'lazypipe';
import inky from 'inky';
import fs from 'fs';
import siphon from 'siphon-media-query';
import path from 'path';
import merge from 'merge-stream';
import beep from 'beepbeep';
import formatHTML from 'gulp-format-html';
import header from 'gulp-header';
import rename from 'gulp-rename';
import through from 'through2';
import yaml from 'js-yaml';

const $ = plugins();

const dartSass = require('gulp-sass')(require('sass'));
dartSass.compiler = require('sass');

// Look for the --production flag
const PRODUCTION = !!yargs.argv.production;
const EMAIL = yargs.argv.to;

// Declar var so that both AWS and Litmus task can use it.
var CONFIG;

// Build the "dist" folder by running all of the below tasks
gulp.task('build', gulp.series(cleanDist, pages, sass, images, inline));

// Build emails, run the server, and watch for file changes
gulp.task('default', gulp.series('build', server, watch));

// Build emails, then send to litmus
gulp.task('litmus', gulp.series('build', creds, aws, litmus));

// Build emails, then send to EMAIL
gulp.task('mail', gulp.series('build', creds, aws, mail));

// Build emails, then zip
gulp.task('zip', gulp.series('build', zip));

// Build the "dist" folder for Java use
gulp.task(
	'java',
	gulp.series(
		cleanDist,
		inkifyLayouts,
		inlinePartialsAndInkifyPages,
		sass,
		images,
		injectStylesIntoLayouts,
		injectStylesIntoPages,
		formatHtmlFiles,
		changeHtmlFilesToHbsFiles,
		sanitizeHbsFiles,
		injectLayoutSpecificCode,
		injectViewSpecificCode,
		removeHtmlFilesFromDist,
		removeCssDirectoryFromDist
	)
);

// Delete the "dist" folder
// This happens every time a build starts
function cleanDist(done) {
	rimraf('dist', done);
}

// Compile layouts, pages, and partials into flat HTML files
// Then parse using Inky templates
function pages() {
	return gulp
		.src(['src/pages/**/*.html', '!src/pages/archive/**/*.html'])
		.pipe(
			panini({
				root: 'src/pages',
				layouts: 'src/layouts',
				partials: 'src/partials',
				helpers: 'src/helpers',
			})
		)
		.pipe(inky())
		.pipe(gulp.dest('dist'));
}


function inlinePartialsAndInkifyPages() {
	const partialsDir = 'src/partials';
	const partials = {};

	// read all available partials to memory
	fs.readdirSync(partialsDir).forEach(partialFile => {
		const partialName = path.basename(partialFile, '.html');
		const partialContent = fs.readFileSync(path.join(partialsDir, partialFile), 'utf-8');
		partials[partialName] = partialContent;
	});

	return gulp
		.src(['src/pages/**/*.html', '!src/pages/index.html', '!src/pages/archive/**/*.html'])
		.pipe(through.obj(function(file, _, cb) {
			let content = file.contents.toString();

			// Inline partials for java consumption
			for (let partialName in partials) {
				// matches for `{{#> PARTIAL_NAME }}{{/PARTIAL_NAME}}`
				const partialBlockRegex = `{{#>\\s*${partialName}\\s*([^}]+)?}}([\\s\\S]*?){{\\/${partialName}}}`
				// matches for `{{> PARTIAL_NAME paramOne=valueOne paramTwo=valueTwo }}` // capturing params in a group
				const partialRegex = `{{>\\s*${partialName}\\s*([^}]+)?}}`

				const regex = new RegExp(`${partialBlockRegex}|${partialRegex}`, 'g');


				// replace partials with their content .. preserving/forwarding params
				content = content.replace(regex, (match, paramStr1, innerContent, paramStr2) => {
					let partialContent = partials[partialName];

					if (innerContent !== undefined) {
						const blockRegex = new RegExp('{{>\\s*@partial-block\\s*}}', 'g')
						partialContent = partialContent.replace(blockRegex, innerContent)
					}

					const paramStr = paramStr1 || paramStr2;

					if (paramStr && paramStr.trim()) {
						// params can be:
						// - key=value
						// - key="string value"
						// - key
						const params = paramStr.trim().match(/(\w+=['"][^'"]+['"]|\w+=\w+|\w+)/g);

						for (const param of params) {
							const [key, value] = param.split('=');
							const paramRegex = new RegExp(key, 'g');
							partialContent = partialContent.replace(paramRegex, value)
						}
					}

					return partialContent;
				})
			}


			file.contents = Buffer.from( content);

			cb(null, file);
		}))
		.pipe(inky())
		.pipe(gulp.dest('dist/views'));
}
function inkifyLayouts() {
	return gulp
		.src(['src/layouts/**/*.html', '!src/layouts/archive/**/*.html'])
		.pipe(inky())
		.pipe(gulp.dest('dist/layouts'));
}
function injectStylesIntoLayouts() {
	const css = fs.readFileSync('dist/css/app.css').toString();

	return gulp
		.src('dist/layouts/**/*.html')
		.pipe($.replace('<!-- <style> -->', `<style>${css}</style>`))
		.pipe($.replace('<link rel="stylesheet" type="text/css" href="{{root}}css/app.css">', ''))
		.pipe(
			$.inlineCss({
				applyStyleTags: true,
				removeStyleTags: true,
				preserveMediaQueries: true,
				removeLinkTags: true,
			})
		)
		.pipe(gulp.dest('dist/layouts'));
}
function injectStylesIntoPages() {
	const css = fs.readFileSync('dist/css/app.css').toString();

	return gulp
		.src('dist/views/**/*.html')
		.pipe(header(`<style>${css}</style>`))
		.pipe(
			$.inlineCss({
				applyStyleTags: true,
				removeStyleTags: true,
				preserveMediaQueries: false,
				removeLinkTags: true,
			})
		)
		.pipe(gulp.dest('dist/views'));
}
function formatHtmlFiles() {
	return gulp
		.src('dist/**/*.html')
		.pipe(formatHTML({ indent_size: 4, indent_with_tabs: true, indent_inner_html: true }))
		.pipe(gulp.dest('dist'));
}
function changeHtmlFilesToHbsFiles() {
	return gulp
		.src('dist/**/*.html')
		.pipe(
			rename(function (path) {
				path.extname = '.hbs';
			})
		)
		.pipe(gulp.dest('dist'));
}
function sanitizeHbsFiles() {
	// remove the "default" layout which is only
	// a required placeholder for panini/dev task
	// everything is generated starting from _V2
	fs.rmSync('dist/layouts/default.hbs');
	return gulp.src('dist/**/*.hbs').pipe($.replace('{{root}}', '{{{staticFileUrlPrefix}}}')).pipe(gulp.dest('dist'));
}
function injectLayoutSpecificCode() {
	return gulp
		.src('dist/layouts/**/*.hbs')
		.pipe($.replace('{{> body}}', '{{#block "body"}}{{/block}}'))
		.pipe(gulp.dest('dist/layouts'));
}
function injectViewSpecificCode() {
	return gulp
		.src(['dist/views/**/*.hbs', '!dist/views/**/subject.hbs'])
		.pipe(viewHbsCodeInjecter())
		.pipe(gulp.dest('dist/views'));
}
function viewHbsCodeInjecter() {
	const frontMatterRegex = /---(.|\n)*---/;
	const hexColorRegex = /(#[0-9a-fA-F]+)/g
	const openPartialBuffer = Buffer.from('{{#partial "body"}}');
	const closePartialBuffer = Buffer.from('\n{{/partial}}');
	const hexColorThemeTokenMap = getReverseColorTokenMap();

	const stream = through.obj(function (file, _encoding, callback) {
		if (file.isStream()) {
			console.error('Streams are not supported!');
			return callback();
		}

		if (file.isBuffer()) {
			const fileAsString = file.contents.toString();
			const frontMatterString = (fileAsString.match(frontMatterRegex) || [])[0];
			const frontMatterObject = yaml.loadAll(frontMatterString)[0];
			const fileAsStringNoFrontMatter = fileAsString.replace(frontMatterRegex, '');
			const fileAsStringNoHexColors = fileAsStringNoFrontMatter.replace(hexColorRegex, (hexColor) => {
				const colorToken = hexColorThemeTokenMap[hexColor];

				if (colorToken) {
					return `{{colors.${colorToken}}}`;
				}

				return hexColor;
			});

			const hbsTemplateBuffer = Buffer.from(fileAsStringNoHexColors);
			const layoutBuffer = Buffer.from(`\n\n{{> layouts/${frontMatterObject.layout}}}`);

			file.contents = Buffer.concat([openPartialBuffer, hbsTemplateBuffer, closePartialBuffer, layoutBuffer]);
		}

		this.push(file);
		callback();
	});

	return stream;
}
function removeHtmlFilesFromDist(done) {
	rimraf('dist/**/*.html', done);
}
function removeCssDirectoryFromDist(done) {
	rimraf('dist/css', done);
}

// Reset Panini's cache of layouts and partials
function resetPages(done) {
	panini.refresh();
	done();
}

// Compile Sass into CSS
function sass() {
	return gulp
		.src('src/assets/scss/app.scss')
		.pipe($.if(!PRODUCTION, $.sourcemaps.init()))
		.pipe(
			dartSass
				.sync({
					includePaths: ['node_modules/foundation-emails/scss'],
				})
				.on('error', dartSass.logError)
		)
		.pipe(
			$.if(
				PRODUCTION,
				$.uncss({
					html: ['dist/**/*.html'],
				})
			)
		)
		.pipe($.if(!PRODUCTION, $.sourcemaps.write()))
		.pipe(gulp.dest('dist/css'));
}

// Copy and compress images
function images() {
	return gulp
		.src(['src/assets/img/**/*', '!src/assets/img/archive/**/*'])
		.pipe($.imagemin())
		.pipe(gulp.dest('./dist/assets/img'));
}

// Inline CSS and minify HTML
function inline() {
	return gulp
		.src('dist/**/*.html')
		.pipe($.if(PRODUCTION, inliner('dist/css/app.css')))
		.pipe(gulp.dest('dist'));
}

// Start a server with LiveReload to preview the site in
function server(done) {
	browser.init({
		server: 'dist',
	});
	done();
}

// Watch for file changes
function watch() {
	gulp.watch('src/pages/**/*.html').on('all', gulp.series(pages, inline, browser.reload));
	gulp.watch(['src/layouts/**/*', 'src/partials/**/*']).on(
		'all',
		gulp.series(resetPages, pages, inline, browser.reload)
	);
	gulp.watch(['../scss/**/*.scss', 'src/assets/scss/**/*.scss']).on(
		'all',
		gulp.series(resetPages, sass, pages, inline, browser.reload)
	);
	gulp.watch('src/assets/img/**/*').on('all', gulp.series(images, browser.reload));
}

// Inlines CSS into HTML, adds media query CSS into the <style> tag of the email, and compresses the HTML
function inliner(css) {
	var css = fs.readFileSync(css).toString();
	var mqCss = siphon(css);

	var pipe = lazypipe()
		.pipe($.inlineCss, {
			applyStyleTags: false,
			removeStyleTags: true,
			preserveMediaQueries: true,
			removeLinkTags: false,
		})
		.pipe($.replace, '<!-- <style> -->', `<style>${mqCss}</style>`)
		.pipe($.replace, '<link rel="stylesheet" type="text/css" href="css/app.css">', '')
		.pipe($.htmlmin, {
			collapseWhitespace: true,
			minifyCSS: true,
		});

	return pipe();
}

// Ensure creds for Litmus are at least there.
function creds(done) {
	var configPath = './config.json';
	try {
		CONFIG = JSON.parse(fs.readFileSync(configPath));
	} catch (e) {
		beep();
		console.log('[AWS]'.bold.red + ' Sorry, there was an issue locating your config.json. Please see README.md');
		process.exit();
	}
	done();
}

// Post images to AWS S3 so they are accessible to Litmus and manual test
function aws() {
	var publisher = !!CONFIG.aws ? $.awspublish.create(CONFIG.aws) : $.awspublish.create();
	var headers = {
		'Cache-Control': 'max-age=315360000, no-transform, public',
	};

	return (
		gulp
			.src('./dist/assets/img/*')
			// publisher will add Content-Length, Content-Type and headers specified above
			// If not specified it will set x-amz-acl to public-read by default
			.pipe(publisher.publish(headers))

			// create a cache file to speed up consecutive uploads
			//.pipe(publisher.cache())

			// print upload updates to console
			.pipe($.awspublish.reporter())
	);
}

// Send email to Litmus for testing. If no AWS creds then do not replace img urls.
function litmus() {
	var awsURL = !!CONFIG && !!CONFIG.aws && !!CONFIG.aws.url ? CONFIG.aws.url : false;

	return gulp
		.src('dist/**/*.html')
		.pipe($.if(!!awsURL, $.replace(/=('|")(\/?assets\/img)/g, '=$1' + awsURL)))
		.pipe($.litmus(CONFIG.litmus))
		.pipe(gulp.dest('dist'));
}

// Send email to specified email for testing. If no AWS creds then do not replace img urls.
function mail() {
	var awsURL = !!CONFIG && !!CONFIG.aws && !!CONFIG.aws.url ? CONFIG.aws.url : false;

	if (EMAIL) {
		CONFIG.mail.to = [EMAIL];
	}

	return gulp
		.src('dist/**/*.html')
		.pipe($.if(!!awsURL, $.replace(/=('|")(\/?assets\/img)/g, '=$1' + awsURL)))
		.pipe($.mail(CONFIG.mail))
		.pipe(gulp.dest('dist'));
}

// Copy and compress into Zip
function zip() {
	var dist = 'dist';
	var ext = '.html';

	function getHtmlFiles(dir) {
		return fs.readdirSync(dir).filter(function (file) {
			var fileExt = path.join(dir, file);
			var isHtml = path.extname(fileExt) == ext;
			return fs.statSync(fileExt).isFile() && isHtml;
		});
	}

	var htmlFiles = getHtmlFiles(dist);

	var moveTasks = htmlFiles.map(function (file) {
		var sourcePath = path.join(dist, file);
		var fileName = path.basename(sourcePath, ext);

		var moveHTML = gulp.src(sourcePath).pipe(
			$.rename(function (path) {
				path.dirname = fileName;
				return path;
			})
		);

		var moveImages = gulp
			.src(sourcePath)
			.pipe($.htmlSrc({ selector: 'img' }))
			.pipe(
				$.rename(function (currentpath) {
					currentpath.dirname = path.join(fileName, currentpath.dirname.replace('dist', ''));
					return currentpath;
				})
			);

		return merge(moveHTML, moveImages)
			.pipe($.zip(fileName + '.zip'))
			.pipe(gulp.dest('dist'));
	});

	return merge(moveTasks);
}
// mapping of '#ffffff: n0'
// reversed from sass '$n0: #ffffff;'
function getReverseColorTokenMap() {
	const colorsFileLines = fs
		.readFileSync("src/assets/scss/_colors.scss", "utf-8")
		.split("\n");

	const colorMap = {}

	for (const line of colorsFileLines) {
		// start of line -- $"someVar" followed by a ':' some whitespace and the #hex;
		const colorDeclarationRegex = new RegExp(
			`^\\$(\\w+):\\s*(#[0-9a-fA-F]+);`
		);
		const match = line.match(colorDeclarationRegex);

		if (match) {
			const [, colorName, colorHex] = match;
			colorMap[colorHex] = colorName;
		}
	}

	return colorMap;
}
