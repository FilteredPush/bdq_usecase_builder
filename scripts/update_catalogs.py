"""Regenerate the bundled catalogs of BDQ Tests and controlled vocabularies.

Writes, under src/main/resources/catalog/:

- bdqtest_catalog.csv: one row per Test in the BDQ standard (iri, label, prefLabel, type, dimension),
  read from the bdqtest graph (current form: each Test by its term IRI);
- vocabulary/{bdqcrit,bdqdim,bdqenh,bdquc,bdqval}.csv: the local names of the current, not deprecated,
  terms of each vocabulary, read from rs.tdwg.org, the source of truth for the BDQ vocabularies.

Usage (from the repository root; needs rdflib):

    python scripts/update_catalogs.py [--bdqtest URL_OR_PATH] [--rs URL_OR_PATH]

--bdqtest defaults to https://bdq.tdwg.org/draft/dist/bdqtest.ttl; --rs to the bdq branch of
tdwg/rs.tdwg.org on GitHub (a local rs.tdwg.org checkout also works). dwc.csv and ac.csv are not changed.
"""
import argparse
import csv
import io
import pathlib
import urllib.request

import rdflib

BDQTEST = 'https://bdq.tdwg.org/draft/dist/bdqtest.ttl'
RS = 'https://raw.githubusercontent.com/tdwg/rs.tdwg.org/bdq'
VOCABULARIES = ['bdqcrit', 'bdqdim', 'bdqenh', 'bdquc', 'bdqval']
BDQFFDQ = rdflib.Namespace('https://rs.tdwg.org/bdqffdq/terms/')
TEST_TYPES = ['Validation', 'Measure', 'Amendment', 'Issue']
CATALOG = pathlib.Path(__file__).resolve().parent.parent / 'src' / 'main' / 'resources' / 'catalog'


def read_text(location):
    if location.startswith(('http://', 'https://')):
        with urllib.request.urlopen(location) as response:
            return response.read().decode('utf-8')
    return pathlib.Path(location).read_text(encoding='utf-8')


def write_csv(path, header, rows):
    with open(path, 'w', newline='', encoding='utf-8') as f:
        writer = csv.writer(f, lineterminator='\n')
        writer.writerow(header)
        writer.writerows(rows)
    print(f'{path.relative_to(CATALOG.parent.parent.parent.parent)}: {len(rows)} rows')


def tests(location):
    graph = rdflib.Graph().parse(data=read_text(location), format='turtle')
    rows = []
    for test_type in TEST_TYPES:
        for test in graph.subjects(rdflib.RDF.type, BDQFFDQ[test_type]):
            dimension = graph.value(test, BDQFFDQ.hasDataQualityDimension)
            rows.append([str(test), str(graph.value(test, rdflib.RDFS.label)),
                         str(graph.value(test, rdflib.namespace.SKOS.prefLabel)), test_type,
                         str(dimension).rsplit('/', 1)[-1] if dimension else ''])
    return sorted(rows, key=lambda row: row[1])


def terms(rs, vocabulary):
    text = read_text(f'{rs.rstrip("/")}/{vocabulary}/{vocabulary}.csv')
    return sorted(row['term_localName'] for row in csv.DictReader(io.StringIO(text))
                  if row.get('term_deprecated', '').strip().lower() != 'true')


def main():
    parser = argparse.ArgumentParser(description=__doc__.split('\n')[0])
    parser.add_argument('--bdqtest', default=BDQTEST, help='bdqtest graph in Turtle (URL or path)')
    parser.add_argument('--rs', default=RS, help='rs.tdwg.org: raw GitHub URL of a branch, or a checkout')
    args = parser.parse_args()
    write_csv(CATALOG / 'bdqtest_catalog.csv', ['iri', 'label', 'prefLabel', 'type', 'dimension'],
              tests(args.bdqtest))
    for vocabulary in VOCABULARIES:
        write_csv(CATALOG / 'vocabulary' / f'{vocabulary}.csv', ['term'],
                  [[term] for term in terms(args.rs, vocabulary)])


if __name__ == '__main__':
    main()
