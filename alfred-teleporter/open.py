import sys
from workflow import Workflow, web


def main(wf):
    query = sys.argv[1]

    params = dict(count=100, format='json')

    res = web.post("http://localhost:8080/teleporter/open",
                   data='["'+query+'"]', params=params)
    res.raise_for_status()

    wf.send_feedback()


if __name__ == "__main__":
    wf = Workflow()
    sys.exit(wf.run(main))
