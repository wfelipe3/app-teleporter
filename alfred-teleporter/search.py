import sys
from workflow import Workflow, web

def command(teleporter):
    return teleporter["app"] + " " + " ".join(teleporter["args"])

def main(wf):
    query = sys.argv[1]

    params = dict(count=100, format='json')

    res = web.get("http://localhost:8080/teleporter?search=" + query, params)
    res.raise_for_status()

    teleporters = res.json()

    for tep in teleporters:
        wf.add_item(title=tep["name"],
                    valid=True,
                    arg=tep["name"],
                    subtitle=command(tep))

    wf.send_feedback()


if __name__ == "__main__":
    wf = Workflow()
    sys.exit(wf.run(main))
